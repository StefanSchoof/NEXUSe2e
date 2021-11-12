import { ComponentFixture, TestBed } from "@angular/core/testing";

import { ListComponent } from "./list.component";
import { MatTableModule } from "@angular/material/table";
import { MatCheckboxModule } from "@angular/material/checkbox";
import { RouterTestingModule } from "@angular/router/testing";
import { TranslateModule } from "@ngx-translate/core";
import { messages } from "../test-data";
import { MatCardModule } from "@angular/material/card";
import { MatDialogModule } from "@angular/material/dialog";
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { ColumnType } from "../types";
import { CapsToTitleCasePipe } from "../pipes/caps-to-title-case.pipe";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { RequestHelper } from "../services/request-helper";
import { DataService } from "../services/data.service";

describe("ListComponent", () => {
  let component: ListComponent;
  let fixture: ComponentFixture<ListComponent>;
  let requestHelper: RequestHelper;
  let dataService: DataService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        MatTableModule,
        MatCheckboxModule,
        MatCardModule,
        RouterTestingModule,
        TranslateModule.forRoot(),
        MatDialogModule,
        MatSnackBarModule,
      ],
      declarations: [ListComponent, CapsToTitleCasePipe],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ListComponent);
    component = fixture.componentInstance;
    component.mobileConfig = [
      {
        columnType: ColumnType.LINK,
        fieldName: "messageId",
        linkUrlRecipe: "../../message/$nxMessageId$",
        isHeader: true,
      },
      {
        columnType: ColumnType.LINK,
        fieldName: "conversationId",
        linkUrlRecipe: "../../conversation/$nxConversationId$",
      },
      { columnType: ColumnType.TEXT, fieldName: "partnerId" },
      { columnType: ColumnType.TEXT, fieldName: "typeName" },
      { columnType: ColumnType.TEXT, fieldName: "actionId" },
      { columnType: ColumnType.TEXT, fieldName: "createdDate" },
    ];
    component.desktopConfig = [
      {
        columnType: ColumnType.LINK,
        fieldName: "messageId",
        linkUrlRecipe: "../../message/$nxMessageId$",
      },
      {
        columnType: ColumnType.LINK,
        fieldName: "conversationId",
        linkUrlRecipe: "../../conversation/$nxConversationId$",
      },
      { columnType: ColumnType.TEXT, fieldName: "partnerId" },
      { columnType: ColumnType.TEXT, fieldName: "status" },
      { columnType: ColumnType.TEXT, fieldName: "backendStatus" },
      { columnType: ColumnType.TEXT, fieldName: "typeName" },
      { columnType: ColumnType.TEXT, fieldName: "choreographyId" },
      { columnType: ColumnType.TEXT, fieldName: "actionId" },
      { columnType: ColumnType.TEXT, fieldName: "createdDate" },
      { columnType: ColumnType.TEXT, fieldName: "turnAroundTime" },
    ];
    component.items = messages;
    component.itemType = "message";
    component.showAsSimpleTable = false;
    requestHelper = TestBed.inject(RequestHelper);
    dataService = TestBed.inject(DataService);
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should show a header element if it is configured in mobile with a linkUrlRecipe", () => {
    spyOn(component.screenSizeService, "isMobile").and.returnValue(true);
    component.mobileConfig = [
      {
        columnType: ColumnType.LINK,
        fieldName: "messageId",
        linkUrlRecipe: "../../message/$nxMessageId$",
        isHeader: true,
      },
    ];
    component.items = messages;
    component.ngOnInit();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector(".card-header")).toBeTruthy();
  });

  it("should get property by propertyName", () => {
    const message = {
      messageId: "640FE4F8-6A6D-B7D8-E35B-FB7FE4764426",
      actionId: "SendFile",
      choreographyId: "GenericFile",
      createdDate: "01-12-2022 15:21:03 GMT",
      typeName: "Normal",
      status: "Processed",
      conversationId: "CEBC3B83-3648-41C6-F033-E8B8A4A48AF8",
      nxMessageId: 1,
      nxConversationId: 1,
      partnerId: "Victor",
      backendStatus: "QUEUED",
      turnAroundTime: "Not terminated",
    };
    const propertyValue = dataService.getProperty(message, "nxConversationId");
    expect(propertyValue).toEqual(message.nxConversationId);
  });

  it("should return trimmed property + '...', if it is called", () => {
    const engineLog = {
      description:
        "Error sending message: 8e59df58-8c39-4821-837b-5dbc71866f1c/85932ca0-13db-4ed2-9127-ecefa100c74e: Message submission failed, server http://localhost:8080/NEXUSe2e/handler/ebxml20 responded with status: 404",
      createdDate: "01-12-2022 15:21:03 GMT",
      severity: "ERROR",
      className: "org.nexuse2e.Engine",
      methodName: "start",
    };
    const trimmedPropertyValue = component.getTrimmedProperty(
      engineLog,
      "description"
    );
    expect(trimmedPropertyValue).toEqual(
      engineLog.description.substr(0, component["longTextThreshold"]) + "..."
    );
  });

  it("should display one column for each configured ColumnConfig item in desktop", () => {
    spyOn(component.screenSizeService, "isMobile").and.returnValue(false);
    fixture.detectChanges();
    const columns = fixture.nativeElement.querySelectorAll("th");
    // display columns contain desktopConfig + select column (even if isSelectable is false)
    expect(columns.length).toEqual(component.desktopConfig.length + 1);
  });

  it("should display one card row for each configured ListConfig item in mobile", () => {
    spyOn(component.screenSizeService, "isMobile").and.returnValue(true);
    fixture.detectChanges();
    const firstCard = fixture.nativeElement.querySelector("mat-card");
    const header = firstCard.querySelectorAll(".card-header");
    const rows = firstCard.querySelectorAll("tr");
    expect(rows.length).toEqual(component.mobileConfig.length - header.length);
  });

  it("should build the item url if it consists of one variable", () => {
    const item = messages[0];
    const recipe = "$nxConversationId$";
    const builtUrl = requestHelper.getUrl(item, recipe);
    expect(builtUrl).toEqual("" + item.nxConversationId);
  });

  it("should build the item url if it contains no variables", () => {
    const item = messages[0];
    const recipe = "/base/test";
    const builtUrl = requestHelper.getUrl(item, recipe);
    expect(builtUrl).toEqual("/base/test");
  });

  it("should build the item url if it contains multiple variables in the middle", () => {
    const item = messages[0];
    const recipe = "/base/test/$nxConversationId$/test2/$nxConversationId$/";
    const builtUrl = requestHelper.getUrl(item, recipe);
    expect(builtUrl).toEqual(
      "/base/test/" +
        item.nxConversationId +
        "/test2/" +
        item.nxConversationId +
        "/"
    );
  });

  it("should build the item url if it contains multiple variables on the edges", () => {
    const item = messages[0];
    const recipe = "$nxConversationId$/test/test3/$nxConversationId$";
    const builtUrl = requestHelper.getUrl(item, recipe);
    expect(builtUrl).toEqual(
      item.nxConversationId + "/test/test3/" + item.nxConversationId
    );
  });
});
