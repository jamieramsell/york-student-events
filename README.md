# york-student-events
 
> A centralised event discovery and social platform exclusively for University of York students.
 
[![Version](https://img.shields.io/badge/version-0.1.0-blue.svg)](CHANGELOG.md)
[![Versioning](https://img.shields.io/badge/versioning-semantic-brightgreen.svg)](https://semver.org)
[![Python](https://img.shields.io/badge/python-3.11+-yellow.svg)](https://www.python.org)
[![Java](https://img.shields.io/badge/java-21+-orange.svg)](https://openjdk.org)
 
---
 
## Overview
 
York Student Events solves a fragmented problem: university events, society meetups, private venue nights, and city-wide activities are scattered across emails, Instagram pages, noticeboards, and separate websites. This platform brings everything onto one hub, visible only to verified University of York students.
 
Beyond event discovery, the platform introduces a cohort-based social layer- think LinkedIn, but stripped back and casual- so a first-year student can find and connect with second-years in the same department, discover mutual interests, and attend events together.
 
---
 
## Features
 
### Planned (future versions)
 
- **Event management** — create, update, and browse events hosted by the university, student societies, private venues, or the city
- **User accounts** — student-verified profiles with cohort metadata (year, department)
- **Subscriptions** — subscribe to event hosts and venues; receive updates when new events are posted
- **Attendance tracking** — mark attendance at events; data feeds into the badge system
- **Badge system** — earn badges based on attendance milestones and event categories
- **Friends system** — send and accept friend requests between student accounts
- **Mutual-interest matching** — algorithm surfaces students with overlapping interests and attendance history
- **Cohort networking** — filter and connect with students by year group and department
- HTTP-served web platform (frontend client)
- Interactive maps of friend networks
- Chat system with message filtering and report/moderation tooling
- Push notifications for subscribed hosts and venues
---
 
## Architecture
 
The backend is to be split across two services:
 
| Service | Language | Responsibility |
|---|---|---|
| `api-core` | Python | Attendance, badges, friend graph, interest matching |
| `event-service` | Java | User accounts, subscriptions, event creation, host/venue management |
 
---
 
## Project Structure
 
```
york-student-events/
│
├── event-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── york/
│   │   │   │       └── studentevents/
│   │   │   │           ├── Application.java
│   │   │   │           ├── events/
│   │   │   │           │   ├── IEvent.java
│   │   │   │           │   ├── Event.java
│   │   │   │           │   ├── EventService.java
│   │   │   │           │   ├── EventController.java
│   │   │   │           │   └── IEventRepository.java
│   │   │   │           ├── users/
│   │   │   │           │   ├── IUser.java
│   │   │   │           │   ├── User.java
│   │   │   │           │   ├── UserService.java
│   │   │   │           │   ├── UserController.java
│   │   │   │           │   └── IUserRepository.java
│   │   │   │           ├── venues/
│   │   │   │           │   ├── IVenue.java
│   │   │   │           │   ├── Venue.java
│   │   │   │           │   ├── VenueService.java
│   │   │   │           │   ├── VenueController.java
│   │   │   │           │   └── IVenueRepository.java
│   │   │   │           ├── subscriptions/
│   │   │   │           │   ├── IObserver.java
│   │   │   │           │   ├── IObservable.java
│   │   │   │           │   └── EventNotificationService.java
│   │   │   │           └── repository/
│   │   │   │               ├── IRepository.java
│   │   │   │               └── inmemory/
│   │   │   │                   ├── InMemoryEventRepository.java
│   │   │   │                   └── InMemoryUserRepository.java
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   │       └── java/
│   │           └── york/
│   │               └── studentevents/
│   │                   ├── events/
│   │                   │   └── EventServiceTest.java
│   │                   ├── users/
│   │                   │   └── UserServiceTest.java
│   │                   └── subscriptions/
│   │                       └── EventNotificationServiceTest.java
│   └── pom.xml
│
├── api-core/
│   ├── src/
│   │   ├── attendance/
│   │   │   └── attendance.py
│   │   ├── badges/
│   │   │   └── badges.py
│   │   ├── friends/
│   │   │   └── getFriendCircle.py
│   │   └── matching/
│   │       └── matching.py
│   └── tests/
│       ├── test_attendance.py
│       ├── test_badges.py
│       ├── test_friends.py
│       └── test_matching.py
│
├── docs/
│   └── api-spec.yaml
│
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── feature.md
│   │   └── bug.md
│   └── pull_request_template.md
│
├── .gitignore
├── CHANGELOG.md
└── README.md
```
 
---
 
## Getting Started
 
### Prerequisites
 
- Python 3.11+
- Java 21+
### Running the Python service
 
---
 
## Versioning
 
This project uses [Semantic Versioning](https://semver.org/). Releases follow the `MAJOR.MINOR.PATCH` format:
 
- `MAJOR` — breaking API changes
- `MINOR` — new backwards-compatible features
- `PATCH` — backwards-compatible bug fixes
See [CHANGELOG.md](CHANGELOG.md) for the full release history.
 
---
 
## Contributing
 
Contributions are welcome from University of York students and staff. Please open an issue before submitting a pull request so the proposed change can be discussed first.
 
1. Fork the repository
2. Create a feature branch (`git checkout -b feat/your-feature`)
3. Commit your changes (`git commit -m 'feat: add your feature'`)
4. Push to the branch (`git push origin feat/your-feature`)
5. Open a pull request

Commit messages should follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.
