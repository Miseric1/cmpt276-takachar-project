# cmpt276-takachar-project

# SupportSync – Customer Feedback & Support Management System

## Project Abstract

SupportSync is a web-based Customer Feedback & Support Management System designed to help Takachar centralize and streamline customer interactions. The system integrates feedback tracking, a searchable knowledge base, and a lightweight customer support ticketing workflow into a single platform.

---

## Problem Statement

Takachar currently lacks a centralized system for managing customer feedback, support requests, and knowledge base documentation. Customer interactions and internal support processes are currently handled in a more manual way, such as Telegram or email. Because of this, there is a need for a centralized system that connects feedback, documentation, and support workflows in one place.

---
## Existing Systems

Existing systems such as Zendesk, Freshdesk, and Jira Service Management already provide customer support, feedback tracking, and ticketing features, but they are often complex and designed for large enterprises. SupportSync improves on this by offering a simpler, unified system that combines feedback, knowledge base, and ticketing workflows in one platform tailored for Takachar.

---

## Proposed Solution

SupportSync will provide an integrated platform consisting of three main modules.

The first module is the Customer Feedback Tracker, where users can submit, categorize, and track customer feedback over time. Admins will be able to view trends, tag recurring issues, and monitor unresolved feedback.

The second module is the FAQ and Knowledge Base System, where team members can add, edit, and manage FAQ entries. Each change will be logged, and updates can notify relevant team members to ensure consistency and transparency in documentation.

The third module is the CRM and Support Ticketing System. Incoming customer issues will be processed through a ticketing system that can automatically match issues with existing FAQ entries using REST API logic. If no match is found or the issue is not resolved, it will be escalated into a support ticket assigned to a human agent.

---

## Target Audience

The target users for this application are Takachar team members and customers of Takachar. The system is designed to support both internal staff managing feedback and external users submitting issues or feedback.

---

## Scope of the Project

This is a full-stack web application with a login-based system and multiple integrated modules. The system includes role-based access control for admin, support agent, and customer roles, allowing structured workflows between feedback, knowledge base, and ticket resolution.

The scope of the project is divided into multiple independent but connected components, making it suitable for a five-member group project where each member is responsible for one major feature.

---

## Epics (Major Features)

The project is divided into five epics, each representing a major feature of the system.

The first epic is Authentication and User Management, which includes user registration, login, and role-based access control for admin, agent, and customer users.

The second epic is the Customer Feedback System, which allows users to submit feedback, categorize it, and track it over time. It also provides a dashboard for viewing feedback trends and recurring issues.

The third epic is the FAQ and Knowledge Base Module, which allows team members to create, edit, and delete FAQ entries. It also includes version tracking and change logs to maintain consistency.

The fourth epic is the Ticketing and CRM Workflow system. This module allows tickets to be created automatically or manually. It matches tickets with FAQ entries using REST API logic, and escalates unresolved issues to human agents.

The fifth epic is Analytics and Dashboard, which displays metrics such as ticket resolution time, feedback trends, and common issues to provide an overview for administrators.

---

## Use of Web APIs

The system will use REST APIs to support both internal functionality and external data integration.

Internal APIs within the Spring Boot backend will handle ticket matching logic and communication between modules.

In addition, the system will integrate the Hugging Face Inference API for sentiment analysis of customer feedback. This API will classify feedback as positive, neutral, or negative, helping administrators prioritize urgent issues and analyze customer satisfaction trends.

---

## Backend Documentation (Iteration 2)

Iteration 2 is a backend-only iteration: it migrates the database to Supabase and
adds the Dashboard, FAQ, and Knowledge Base backends. Details live in `docs/`:

- [`docs/BACKEND_ITERATION_2.md`](docs/BACKEND_ITERATION_2.md) — architecture, package map, and database schema.
- [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md) — every new REST endpoint, request/response shapes, auth, pagination, and error format (frontend integration guide).
- [`docs/SUPABASE_SETUP.md`](docs/SUPABASE_SETUP.md) — Supabase migration, environment variables, and Docker/Render deployment.

The project targets Java 17 (Spring Boot 3.2.5). Run `mvn test` to verify; use
`SPRING_PROFILES_ACTIVE=prod` with the Supabase env vars to run against Supabase,
or no profile for local H2.

--- 
