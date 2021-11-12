import { ComponentFixture, TestBed } from "@angular/core/testing";

import { FilterPanelComponent } from "./filter-panel.component";
import { By } from "@angular/platform-browser";
import { TranslateModule } from "@ngx-translate/core";
import { MatIconModule } from "@angular/material/icon";
import { SelectFilterComponent } from "../select-filter/select-filter.component";
import { TextFilterComponent } from "../text-filter/text-filter.component";
import { DateRangeFilterComponent } from "../date-range-filter/date-range-filter.component";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { MatSelectModule } from "@angular/material/select";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatNativeDateModule } from "@angular/material/core";
import { FormsModule } from "@angular/forms";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { ScreensizeService } from "../services/screensize.service";
import { StringPipe } from "../pipes/string.pipe";
import { DateRangePipe } from "../pipes/date-range.pipe";
import { FilterType } from "../types";
import { ActivatedRoute } from "@angular/router";

describe("FilterPanelComponent", () => {
  let component: FilterPanelComponent;
  let fixture: ComponentFixture<FilterPanelComponent>;
  let screensizeService: ScreensizeService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [
        FilterPanelComponent,
        SelectFilterComponent,
        TextFilterComponent,
        DateRangeFilterComponent,
        StringPipe,
        DateRangePipe,
      ],
      imports: [
        TranslateModule.forRoot(),
        FormsModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatDatepickerModule,
        MatNativeDateModule,
        BrowserAnimationsModule,
        MatAutocompleteModule,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: {},
              queryParamMap: {
                get: () => {
                  "startEndDateRange";
                },
              },
            },
          },
        },
      ],
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(FilterPanelComponent);
    component = fixture.componentInstance;
    screensizeService = TestBed.inject(ScreensizeService);
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });

  it("should display the number of active filters when collapsed and if mobile view", () => {
    spyOn(component.sessionService, "getActiveFilters").and.returnValue({
      filter1: "1",
      filter2: "2",
    });
    spyOn(screensizeService, "isMobile").and.returnValue(true);
    fixture.detectChanges();

    const activeFiltersBadge = fixture.debugElement.query(
      By.css(".active-filters-badge")
    );

    expect(activeFiltersBadge.nativeElement.textContent).toBe("2");
  });

  it("should render all filters specified in the config object", () => {
    component.toggleVisibility();
    component.filters = [
      {
        fieldName: "status",
        filterType: FilterType.SELECT,
        allowedValues: ["FAILED", "PROCESSED"],
      },
      {
        fieldName: "conversationId",
        filterType: FilterType.TEXT,
      },
      {
        fieldName: "startEndDateRange",
        filterType: FilterType.DATE_TIME_RANGE,
      },
    ];
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll(".filter").length).toBe(3);
  });

  it("should emit an event including the active filters when the filter button is clicked", () => {
    component.toggleVisibility();
    fixture.detectChanges();
    spyOn(component.filterChange, "emit");
    const button = fixture.nativeElement.querySelector("#filterButton");

    button.dispatchEvent(new Event("click"));
    fixture.detectChanges();

    expect(component.filterChange.emit).toHaveBeenCalledWith(
      component.activeFilters
    );
  });

  it("should add 1 to active filter count if active filters contain start and end date in one DateRange object", () => {
    component.activeFilters["startEndDateRange"] = {
      startDate: new Date(),
      endDate: new Date(),
    };
    component.activeFilters["type"] = "Completed";

    expect(component.getNumberOfActivatedFilters()).toEqual(3);
  });

  it("should not add 1 to active filter count if active filters contain only one date in the DateRange object", () => {
    component.activeFilters["startEndDateRange"] = {
      startDate: new Date(),
      endDate: undefined,
    };
    component.activeFilters["type"] = "Completed";

    expect(component.getNumberOfActivatedFilters()).toEqual(2);
  });

  it("should reset filters to default values when clicking reset button", () => {
    component.filters = [
      {
        fieldName: "filter1",
        filterType: FilterType.TEXT,
        defaultValue: "NORMAL",
      },
      {
        fieldName: "filter2",
        filterType: FilterType.TEXT,
      },
    ];
    component.activeFilters = {
      filter1: "1",
      filter2: "2",
    };
    component.resetFiltersAndSetDefaults();

    expect(component.activeFilters).toEqual({ filter1: "NORMAL" });
  });
});
