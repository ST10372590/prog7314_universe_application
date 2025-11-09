# UniVerse - Student Engagement & Learning Platform
## About the Project

UniVerse is a comprehensive educational platform designed to enhance the learning experience of students, improve communication between students and instructors, and provide tools for better academic performance tracking. The system leverages modern technologies such as role-based dashboards, gamification, messaging, and calendar management to create an engaging and organized learning environment.

This project includes an Android front-end application and a RESTful API backend that handles data management, authentication, and communication features.

## About the API

### The UniVerse API provides endpoints for:

- Authentication: Sign-up, login, logout, and password reset, supporting multiple authentication methods including email/password, Google SSO, and biometric login.

- Courses & Modules: Fetch all courses, enrolled courses, and course modules.

- Claim & Submissions: Submit and track academic claims and assignments.

- Messaging & Gamification (innovative features): Track points, badges, streaks, and enable messaging between users (details below).

- The API is built with RESTful principles, ensuring secure, scalable, and modular interaction between the mobile application and backend.

## Innovative Features Added in UniVerse

### UniVerse includes several unique features to enhance engagement and usability:

Gamification

   -Students earn points, badges, and streaks based on their activity and engagement.

  - A leaderboard system encourages friendly competition.

  - Visual progress indicators motivate students to stay consistent in their studies.

### Messaging

  - Direct messaging between students and lecturers.

  - Group messaging for class discussions.

  - Announcements from lecturers visible to all students in the course.

### Role-Based Dashboards

  - Users see a dashboard tailored to their role.

  - Students: Can view timetables, assignments, submissions, and progress.

  - Lecturers: Can manage assignments, view student progress, and post announcements.

  - Role-based access control ensures users can only interact with features relevant to their role.

### Calendar & Deadlines

  - Unified calendar displaying assignment deadlines, exams, and events.

  - Events can be color-coded by type for better organization.

  - Synchronization with device calendars ensures students never miss deadlines.

### Features Planned for Future Implementation

The following features are planned to be added in upcoming versions:

  - Announcements

  - Notifications

  - Group Messaging Enhancements

  - Progress Tracking Improvements

  - Offline Mode Support for viewing content and submitting assignments while disconnected.

### Functional Requirements Covered

  - Authentication: Strong authentication with secure credential storage and account recovery.

  - Multi-language Support: Interface available in English, Afrikaans, isiZulu, and Sesotho.

  - Settings/Profile: Users can manage personal information, notifications, and privacy settings.

## How to Run the App
Prerequisites

Android Studio installed (Arctic Fox or later recommended).

Android SDK and required build tools installed.

Internet access to connect to the backend API.

## Steps

### Clone the repository:

https://github.com/VCPTA/bca3-prog7314-part-2-submission-ST10372590.git


### Open the project in Android Studio.

Sync Gradle and ensure all dependencies are downloaded.

Configure the API endpoint in ApiClient.kt if needed.

Run the app on an emulator or physical device:

Select a target device.

Press Run (Shift + F10).

Use the login screen to sign in as a student or lecturer.

### Explore the features:

- Gamification: Track points, streaks, and badges.

- Role-Based Dashboards: Access features according to your role.

- Messaging: Communicate with other users.

- Calendar: View course deadlines and events.

## Notes

- Some features, such as offline mode and notifications, will be added in future updates.

- Ensure Google Sign-In is properly configured for SSO login.

- The system uses secure JWT authentication; the token is stored in the app for authorized requests.
