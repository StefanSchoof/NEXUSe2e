import { Component, Input, OnInit } from "@angular/core";
import { DataService } from "../services/data.service";
import { ScreensizeService } from "../services/screensize.service";

@Component({
  selector: "app-conversation-status-counts",
  templateUrl: "./conversation-status-counts.component.html",
  styleUrls: ["./conversation-status-counts.component.scss"],
})
export class ConversationStatusCountsComponent implements OnInit {
  @Input() dashboardTimeFrameInDays = 0;
  conversationStatusCounts: { [status: string]: number } = {};
  statusCountOver0 = 0;
  totalCount = 0;
  objectKeys = Object.keys;
  loaded = false;

  constructor(
    private dataService: DataService,
    public screenSizeService: ScreensizeService
  ) {}

  ngOnInit(): void {
    void this.updateView();
  }

  getConversationStatusTotalCount() {
    if (Object.keys(this.conversationStatusCounts).length > 0) {
      for (const conversationStatusCountsKey in this.conversationStatusCounts) {
        this.totalCount =
          this.totalCount +
          this.conversationStatusCounts[conversationStatusCountsKey];
      }
    }
  }

  async updateView() {
    this.loaded = false;
    this.conversationStatusCounts = await this.dataService.getConversationStatusCounts();
    this.getConversationStatusTotalCount();
    this.statusCountOver0 = this.countStatusCountsOver0();
    this.loaded = true;
  }

  countStatusCountsOver0(): number {
    let statusCount = 0;
    if (Object.keys(this.conversationStatusCounts).length > 0) {
      for (const conversationStatusCountsKey in this.conversationStatusCounts) {
        if (this.conversationStatusCounts[conversationStatusCountsKey] > 0) {
          statusCount++;
        }
      }
    }
    return statusCount;
  }
}
