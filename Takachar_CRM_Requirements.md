**Customer Feedback & Support Management System**

Product Requirements Document

Prepared for: Takachar | Prepared by: Group 14, SFU

# **1\. Project Overview**

Takachar requires a centralised web-based dashboard that manages customer complaints and feedback from end-to-end. The system must bring together three core pillars: a structured feedback tracker, an intelligent FAQ and knowledge base, and a full CRM and support ticketing flow. Every stage of a complaint's lifecycle will be visible in real time to the relevant stakeholders, with colour-coded timelines and automated notifications keeping teams aligned.

| Project Name | Customer Feedback & Support Management System |
| :---- | :---- |
| **Client** | Takachar |
| **Client Contact** | Mohammad Irfan |
| **Development Team** | Group 14 \- Simon Fraser University (SFU) |
| **Faculty Advisor** | Bobby Chan, Senior Lecturer, Computing Science, SFU |
| **Document Version** | 1.0 \- Initial Requirements Draft |

# **2\. User Roles and Access**

The system will support two distinct user roles, each with a dedicated view.

## **2.1 Admin / Internal Team View**

* Full visibility across all complaints, feedback entries, and ticket statuses

* Access to assign tickets to departments and update timelines

* Can view all SPOC activity and escalation history

* Has access to the FAQ management panel with change-log

## **2.2 Customer View**

* Customers can only see their own submitted complaints and feedback

* Can track the current status and timeline of their open tickets

* Receives email notifications on key status changes

* Can mark a resolved issue as closed once satisfied

# **3\. Authentication and Login**

Each user (customers) will have individual login credentials. User accounts will be provisioned and managed by the admin. After logging in, customers will be guided through the complaint submission flow as described in Section 5\.

# **4\. Module 1: Customer Feedback Tracker**

This module provides a structured interface for logging, organising, and monitoring customer feedback over time.

* Feedback entries can be created by customers or logged manually by internal staff

* Each entry is tagged by category, date, and project/account

* The admin dashboard surfaces recurring themes and highlights long-standing unresolved items

* Feedback history is retained for trend analysis and reporting

# **5\. Module 2: FAQ and Knowledge Base**

A maintained, searchable knowledge base that serves as the first line of resolution for common issues.

* Organised by topic and searchable by keyword

* Each article carries a version history and change log

* When an article is added or updated, relevant team members receive an automated notification

* The FAQ is directly integrated into the complaint submission flow (see Section 6\) to attempt automatic resolution before a ticket is raised

# **6\. Module 3: CRM and Support Ticketing**

This is the core complaint management flow. It covers everything from the moment a customer begins reporting an issue through to final resolution and closure.

## **6.1 Guided Complaint Submission (Diagnostic Question Tree)**

When a customer logs in and initiates a complaint, they are walked through a structured sequence of diagnostic questions. The questions follow a branching logic, similar to a decision tree, where each answer narrows down the likely cause of the issue. For example:

* The first question establishes the general area of the problem

* Based on the answer, a follow-up question is presented that drills deeper

* This continues until either a resolution is suggested automatically (via the FAQ match) or the root of the issue has been sufficiently described for a human agent to act on 

The dummy data supplied by Takachar will be structured as a set of question-and-answer pairs arranged in this branching format, covering common complaint scenarios.

## **6.2 Automatic Resolution Attempt**

* If the diagnostic flow finds a matching FAQ article that resolves the issue, the customer is presented with the solution directly

* If satisfied, the session ends without a ticket being raised

* If the issue remains unresolved, the flow proceeds to ticket creation

## **6.3 Ticket Creation and Logging**

* The full question-and-answer trail from the diagnostic flow is attached to the ticket automatically

* The customer has the option to attach supporting images or screenshots before submitting

* On submission, the ticket is assigned a unique reference number and a target resolution date

## **6.4 SPOC Notification and Assignment**

Each project at Takachar has a designated Single Point of Contact (SPOC). On ticket creation:

* An automated email is sent to the relevant project SPOC with a summary of the complaint, the diagnostic trail, and the ticket reference

* The SPOC reviews the ticket and assigns it to the appropriate internal department or team member

* All assignment actions are recorded on the dashboard

## **6.5 Timeline Tracking with Target Dates and Colour Coding**

All stages of a ticket's lifecycle are tracked on the dashboard with timestamps and target dates. The timeline is colour-coded to give an at-a-glance view of health:

| Green | Resolved on time or ahead of target date |
| :---- | :---- |
| **Yellow** | Work in progress, within the target window |
| **Red** | Overdue \- target date has passed without resolution |

The timeline is visible to both the admin (full view across all tickets) and the customer (their own ticket only). Every status update, assignment, and department action is recorded as a time-stamped event on the timeline.

## **6.6 Departmental Workflow**

* The assigned department receives the ticket and works on the resolution

* At each stage, the team updates the ticket status and the timeline entry

* If the issue cannot be resolved by the target date, the department is required to log a reason and provide a progress report, which is visible to the SPOC

* Once resolved, the team raises a completion flag on the ticket

## **6.7 Logistics Handoff and Final Closure**

* Where applicable (e.g. physical delivery, field service), resolved tickets are handed off to the logistics team

* Logistics confirms fulfilment and provides a final update to the SPOC

* The SPOC then communicates the resolution to the customer

* The customer reviews the outcome and marks the ticket as closed from their view

* The closed status and all history are reflected on both the admin and customer dashboards

# **7\. Additional Recommended Features**

Beyond the core requirements above, the following additions would significantly improve the product experience and are recommended for consideration during development.

* Search and filter on the admin dashboard by date range, project, department, status, or SPOC

* Dashboard summary cards showing key metrics: total open tickets, average resolution time, overdue count, and customer satisfaction rate

* Email notification templates (to be defined with Us) for ticket creation, status changes, and resolution

* Audit trail for every action taken on a ticket, including who changed what and when

* Export functionality to download ticket data and feedback reports as CSV or PDF for management review

* Mobile-responsive layout so field staff and customers can access the system from phones

# **8\. Dummy Data Requirements**

We will supply the following dummy data to support development and testing:

* A set of question-and-answer pairs structured as a branching decision tree, covering common complaint categories

* Existing feedback log entries to populate the Feedback Tracker

* Sample FAQ articles.

* Example tickets at various stages (open, in-progress, overdue, resolved) for timeline and colour-coding tests

# 

*This document is a living draft. All requirements are subject to change following consultation with Takachar.*