import { BrowserModule } from "@angular/platform-browser";
import { NgModule } from "@angular/core";
import {
  HTTP_INTERCEPTORS,
  HttpClient,
  HttpClientModule,
} from "@angular/common/http";
import { RouterModule, Routes } from "@angular/router";

import { AppComponent } from "./app.component";
import { LoginComponent } from "./login/login.component";
import { DashboardComponent } from "./dashboard/dashboard.component";
import { AuthGuardService } from "./services/auth-guard.service";
import { FormsModule } from "@angular/forms";
import { PageNotFoundComponent } from "./page-not-found/page-not-found.component";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateLoader, TranslateModule } from "@ngx-translate/core";
import { TranslateHttpLoader } from "@ngx-translate/http-loader";
import { MatInputModule } from "@angular/material/input";
import { MatButtonModule } from "@angular/material/button";
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from "@angular/material/form-field";
import { HeaderComponent } from "./header/header.component";
import { NavigationComponent } from "./navigation/navigation.component";
import { MatIconModule } from "@angular/material/icon";
import { SessionPanelComponent } from "./session-panel/session-panel.component";
import { ReportingComponent } from "./reporting/reporting.component";
import { TransactionReportingComponent } from "./transaction-reporting/transaction-reporting.component";
import { EngineLogComponent } from "./engine-log/engine-log.component";
import { MenuItemComponent } from "./menu-item/menu-item.component";
import { MessagesComponent } from "./messages/messages.component";
import { ConversationsComponent } from "./conversations/conversations.component";
import { MatTabsModule } from "@angular/material/tabs";
import { MatCardModule } from "@angular/material/card";
import { MatCheckboxModule } from "@angular/material/checkbox";
import {
  MatPaginatorIntl,
  MatPaginatorModule,
} from "@angular/material/paginator";
import { AuthInterceptor } from "./auth-interceptor";
import { CustomPaginatorFormatting } from "./custom-paginator-formatting";
import { PaginatedListComponent } from "./paginated-list/paginated-list.component";
import { FilterPanelComponent } from "./filter-panel/filter-panel.component";
import { MatSelectModule } from "@angular/material/select";
import { SelectFilterComponent } from "./select-filter/select-filter.component";
import { MatTableModule } from "@angular/material/table";
import { TextFilterComponent } from "./text-filter/text-filter.component";
import { DateRangeFilterComponent } from "./date-range-filter/date-range-filter.component";
import { MatDatepickerModule } from "@angular/material/datepicker";
import { MatNativeDateModule } from "@angular/material/core";
import { LoadingSpinnerComponent } from "./loading-spinner/loading-spinner.component";
import { MatAutocompleteModule } from "@angular/material/autocomplete";
import { MasterSelectComponent } from "./master-select/master-select.component";
import { ListComponent } from "./list/list.component";
import { StringPipe } from "./pipes/string.pipe";
import { DateRangePipe } from "./pipes/date-range.pipe";
import { ActionButtonComponent } from "./action-button/action-button.component";
import { MatSnackBarModule } from "@angular/material/snack-bar";
import { NotificationComponent } from "./notification/notification.component";
import { SimpleTableDialogComponent } from "./simple-table-dialog/simple-table-dialog.component";
import { ClipboardModule } from "@angular/cdk/clipboard";
import { MatDialogModule } from "@angular/material/dialog";
import { UserConfirmationDialogComponent } from "./user-confirmation-dialog/user-confirmation-dialog.component";
import { ConversationDetailComponent } from "./conversation-detail/conversation-detail.component";
import { MessageDetailComponent } from "./message-detail/message-detail.component";
import { MessagesSuccessfulComponent } from "./messages-successful/messages-successful.component";
import { ConversationStatusCountsComponent } from "./conversation-status-counts/conversation-status-counts.component";
import { CapsToTitleCasePipe } from "./pipes/caps-to-title-case.pipe";
import { MessagesFailedComponent } from "./messages-failed/messages-failed.component";
import { ConversationsIdleComponent } from "./conversations-idle/conversations-idle.component";
import { CertificatesComponent } from "./certificates/certificates.component";
import { MatChipsModule } from "@angular/material/chips";
import { RefreshButtonComponent } from "./refresh-button/refresh-button.component";
import { NgxPullToRefreshModule } from "ngx-pull-to-refresh";

const routes: Routes = [
  { path: "", redirectTo: "/dashboard", pathMatch: "full" },
  { path: "login", component: LoginComponent },
  {
    path: "dashboard",
    component: DashboardComponent,
    canActivate: [AuthGuardService],
    data: {
      title: "Dashboard",
    },
  },
  {
    path: "reporting",
    component: ReportingComponent,
    canActivate: [AuthGuardService],
    data: {
      title: "Reporting",
    },
    children: [
      { path: "", redirectTo: "transaction-reporting", pathMatch: "full" },
      {
        path: "transaction-reporting",
        component: TransactionReportingComponent,
        canActivate: [AuthGuardService],
        data: {
          title: "Transaction Reporting",
        },
        children: [
          { path: "", redirectTo: "conversations", pathMatch: "full" },
          {
            path: "conversations",
            component: ConversationsComponent,
            canActivate: [AuthGuardService],
            data: {
              title: "Conversations",
            },
          },
          {
            path: "messages",
            component: MessagesComponent,
            canActivate: [AuthGuardService],
            data: {
              title: "Messages",
            },
          },
        ],
      },
      {
        path: "conversation/:id",
        component: ConversationDetailComponent,
        canActivate: [AuthGuardService],
      },
      {
        path: "message/:id",
        component: MessageDetailComponent,
        canActivate: [AuthGuardService],
      },
      {
        path: "engine-log",
        component: EngineLogComponent,
        canActivate: [AuthGuardService],
        data: {
          title: "Engine Log",
        },
      },
    ],
  },
  { path: "**", component: PageNotFoundComponent },
];

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, "./assets/i18n/");
}

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    DashboardComponent,
    PageNotFoundComponent,
    HeaderComponent,
    NavigationComponent,
    SessionPanelComponent,
    ReportingComponent,
    TransactionReportingComponent,
    EngineLogComponent,
    MenuItemComponent,
    MessagesComponent,
    ConversationsComponent,
    PaginatedListComponent,
    FilterPanelComponent,
    SelectFilterComponent,
    TextFilterComponent,
    DateRangeFilterComponent,
    MasterSelectComponent,
    LoadingSpinnerComponent,
    ListComponent,
    ActionButtonComponent,
    StringPipe,
    DateRangePipe,
    CapsToTitleCasePipe,
    SimpleTableDialogComponent,
    NotificationComponent,
    UserConfirmationDialogComponent,
    ConversationDetailComponent,
    MessageDetailComponent,
    MessagesSuccessfulComponent,
    ConversationStatusCountsComponent,
    ConversationsIdleComponent,
    MessagesFailedComponent,
    CertificatesComponent,
    RefreshButtonComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    TranslateModule.forRoot({
      defaultLanguage: "en",
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [HttpClient],
      },
    }),
    HttpClientModule,
    RouterModule.forRoot(routes, {
      useHash: true,
      scrollPositionRestoration: "enabled",
    }),
    FormsModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatCardModule,
    MatTableModule,
    MatCheckboxModule,
    MatPaginatorModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    BrowserAnimationsModule,
    MatAutocompleteModule,
    MatSnackBarModule,
    ClipboardModule,
    MatDialogModule,
    MatChipsModule,
    NgxPullToRefreshModule,
  ],
  providers: [
    {
      provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: {
        appearance: "outline",
      },
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true,
    },
    {
      provide: MatPaginatorIntl,
      useClass: CustomPaginatorFormatting,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
