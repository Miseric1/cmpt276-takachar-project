# cmpt276-takachar-project

Group 14 – Customer Feedback & Support Management System
Project Abstract
SupportSync is a web-based Customer Feedback & Support Management System designed to help Takachar centralize and streamline customer interactions. The system integrates feedback tracking, a searchable knowledge base, and a lightweight customer support ticketing workflow into a single platform. 
Problem Statement
Takachar currently lacks a centralized system for managing customer feedback, support requests, and knowledge base documentation. Currently, customer interactions and internal support processes are handled in a more manual way, such as Telegram or email. 
There is a need for a centralized system that connects feedback, documentation, and support workflows in one place.
 
Proposed Solution
SupportSync will provide three main integrated modules:
1. Customer Feedback Tracker
Users can submit, categorize, and track customer feedback over time. Admins can view trends, tag recurring issues, and monitor unresolved feedback.
2. FAQ & Knowledge Base System
A dynamic FAQ system where team members can add or update entries. Each change is logged, and updates notify relevant team members to ensure consistency.
3. CRM & Support Ticketing System
Incoming customer issues are processed through a ticketing system that attempts to automatically match the issue with existing FAQ entries. If no match is found or the issue remains unresolved, it escalates into a support ticket assigned to a human agent.
 
Target Audience
The target users for this application are:
•	Takachar team members
•	Customers of Takachar
 
Scope of the Project
This is a full-stack web application built with a login-based system and multiple integrated modules. The system will include user roles (e.g., admin, support agent, customer) and allow structured workflows between feedback, knowledge base, and ticket resolution.
The scope is divided into multiple independent but connected components suitable for a 5-member group project.
 
Epics (Major Features)
Work Distribution: Each epic will be assigned to one team member, ensuring balanced workload across authentication, feedback management, knowledge base, ticketing workflow, and analytics. 
Epic 1: Authentication & User Management
•	User registration and login system
•	Role-based access control (admin, agent, customer)
Epic 2: Customer Feedback System
•	Submit and categorize feedback
•	View feedback dashboard
•	Track recurring issues
Epic 3: FAQ / Knowledge Base Module
•	Create, edit, and delete FAQ entries
•	Version tracking and change logs
•	Searchable knowledge base
Epic 4: Ticketing & CRM Workflow
•	Create support tickets automatically or manually
•	Match tickets to FAQ entries using REST API logic
•	Escalation system for unresolved issues
Epic 5: Analytics & Dashboard
•	Display metrics such as ticket resolution time
•	Show feedback trends and common issues
•	Admin dashboard overview
 
Use of Web APIs
The system will use REST APIs to support both internal functionality and external data enrichment.
•	Handle ticket matching logic using internal API endpoints within the Spring Boot backend.
•	Integrate the Hugging Face Inference API for sentiment analysis of customer feedback. This API will classify submitted feedback as positive, neutral, or negative, helping administrators prioritize urgent issues and analyze customer satisfaction trends.
All API communication will be performed using HTTP requests within the Spring Boot backend, ensuring modularity and separation of concerns between services.
 
 
