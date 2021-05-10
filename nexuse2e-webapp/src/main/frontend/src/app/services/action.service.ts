import { Injectable } from "@angular/core";
import { DataService } from "./data.service";
import { SelectionService } from "./selection.service";
import { Conversation, Message, NotificationItem } from "../types";
import { MatSnackBar } from "@angular/material/snack-bar";
import { NotificationComponent } from "../notification/notification.component";

@Injectable({
  providedIn: "root",
})
export class ActionService {
  constructor(
    private dataService: DataService,
    private selectionService: SelectionService,
    private _snackBar: MatSnackBar
  ) {}

  async stopMessages() {
    const messages = this.selectionService.getSelectedItems("message");
    try {
      await this.dataService.stopMessages(
        messages.map((m) => (m as Message).messageId)
      );
    } catch {
      this._snackBar.openFromComponent(NotificationComponent, {
        duration: 5000,
        data: {
          snackType: "error",
          textLabel: "messageStatusError",
        } as NotificationItem,
      });
    }
  }

  async requeueMessages() {
    const messages = this.selectionService.getSelectedItems("message");
    try {
      await this.dataService.requeueMessages(
        messages.map((m) => (m as Message).messageId)
      );
    } catch {
      this._snackBar.openFromComponent(NotificationComponent, {
        duration: 5000,
        data: {
          snackType: "error",
          textLabel: "messageStatusError",
        } as NotificationItem,
      });
    }
  }

  async deleteConversations() {
    const conversations = this.selectionService.getSelectedItems(
      "conversation"
    );
    try {
      await this.dataService.deleteConversations(
        conversations.map((c) => (c as Conversation).conversationId)
      );
    } catch {
      this._snackBar.openFromComponent(NotificationComponent, {
        duration: 5000,
        data: {
          snackType: "error",
          textLabel: "conversationDeleteError",
        } as NotificationItem,
      });
    }
  }
}
