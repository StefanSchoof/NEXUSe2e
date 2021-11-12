import {
  Action,
  ActiveFilterList,
  ColumnConfig,
  ColumnType,
  Filter,
  FilterType,
  Separator,
} from "../types";

// START FILTERS
const START_DATE_DEFAULT: Date = new Date(new Date().setHours(0, 0, 0, 0));
const END_DATE_DEFAULT: Date = new Date(new Date().setHours(24, 0, 0, 0));

export const participantFilter: Filter = {
  fieldName: "partnerId",
  filterType: FilterType.TEXT,
};

export const choreographyFilter: Filter = {
  fieldName: "choreographyId",
  filterType: FilterType.TEXT,
};

export const MESS_LIST__FILTERS = [
  {
    fieldName: "startEndDateRange",
    filterType: FilterType.DATE_TIME_RANGE,
    defaultValue: {
      startDate: START_DATE_DEFAULT,
      endDate: END_DATE_DEFAULT,
    },
  },
  {
    fieldName: "messageId",
    filterType: FilterType.TEXT,
  },
  {
    fieldName: "conversationId",
    filterType: FilterType.TEXT,
  },
  choreographyFilter,
  participantFilter,
  {
    fieldName: "status",
    filterType: FilterType.SELECT,
    allowedValues: [
      "FAILED",
      "PROCESSED",
      "UNKNOWN",
      "RETRYING",
      "QUEUED",
      "STOPPED",
    ],
  },
  {
    fieldName: "type",
    filterType: FilterType.SELECT,
    allowedValues: ["NORMAL", "ACKNOWLEDGEMENT", "ERROR"],
    defaultValue: "NORMAL",
  },
];
export const activeFilters: ActiveFilterList = {};
// END FILTERS

// START DISPLAY CONFIG
export const MESS_LIST__DEFAULT_PAGE_SIZE = 20;

export const MESS_LIST__DESKTOP_CONFIG: ColumnConfig[] = [
  { columnType: ColumnType.TEXT, fieldName: "status", titleCase: true },
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
  {
    columnType: ColumnType.ICON,
    fieldName: "typeName",
    label: "messageType",
    iconUrlRecipe: "assets/img/message_type_$typeName$.png",
  },
  {
    columnType: ColumnType.TEXT_AND_MORE,
    fieldName: "choreographyId",
    additionalFieldName: "actionId",
    label: "step",
    separator: Separator.VERTICAL_BAR,
  },
  { columnType: ColumnType.TEXT, fieldName: "createdDate" },
  { columnType: ColumnType.TEXT, fieldName: "turnAroundTime" },
];

export const MESS_LIST__MOBILE_CONFIG: ColumnConfig[] = [
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
  { columnType: ColumnType.TEXT, fieldName: "typeName", label: "messageType" },
  {
    columnType: ColumnType.TEXT_AND_MORE,
    fieldName: "choreographyId",
    additionalFieldName: "actionId",
    label: "step",
    separator: Separator.VERTICAL_BAR,
  },
  { columnType: ColumnType.TEXT, fieldName: "createdDate" },
];
// END DISPLAY CONFIG

// START ACTIONS
export const MESS_LIST__ACTIONS: Action[] = [
  {
    label: "requeue",
    icon: "refresh",
    actionKey: "/messages/requeue",
  },
  {
    label: "stop",
    icon: "stop",
    actionKey: "/messages/stop",
  },
];
// END ACTIONS
