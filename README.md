<p align="center">
  <img src="https://github.com/VCPTA/bca3-prog7314-poe-submission-ST10372590/blob/main/Universe_app_logo.jpeg" width="200" />
</p>

# Universe — Mobile Student Engagement & Learning Platform

Universe is a comprehensive educational platform designed to streamline communication, academic engagement, and real-time student–lecturer interaction within a university environment, and provide tools for better academic performance tracking. The system leverages modern technologies such as role-based dashboards, gamification, messaging, and calendar management to create an engaging and organized learning environment.

This project includes an Android front-end application and a RESTful API backend that handles data management, authentication, and communication features.

## Comprehensive Report

### 1. Purpose of the Application

The UniVerse Mobile Application is built to improve some challenges faced in university environments regarding communication, content delivery, student engagement, and academic tracking. The purpose is to unify fragmented tools into one cohesive mobile platform that enables:

**Core Objectives**

- Enhancing communication between students and lecturers
- Centralizing academic resources, modules, and announcements
- Improving lecturer workflows through structured submission review
- Encouraging engagement through leaderboards and messaging
- Supporting real-time updates via push notifications
- Providing a modern, intuitive, mobile-first experience

**Problems UniVerse Solves**

- Students missing important announcements
- Lecturers lacking a streamlined way to manage submissions
- Poor module content accessibility on mobile devices
- Difficulty fostering communication within classes
- Lack of a unified mobile platform that integrates messaging, learning content, and notifications
- Universe addresses all of these by offering a feature-rich, reliable, and scalable academic companion app.

### 2. Design Considerations

The application's design focuses on usability, modularity, scalability, and performance.

- **A. User Experience (UX) Design**
  - Clean interface with minimal clutter
  - Role-based dashboards (Student / Lecturer)
  - Consistent UI language across all screens
  - Reusable layouts to maintain design consistency
  - Responsive layouts for multiple screen sizes

- **B. Architectural Design**
  - The architecture follows modern Android development principles:
    - Separation of concerns
    - Retrofit for API communication
    - Adapters for dynamic data lists
    - Centralized network utilities
    - Firebase Messaging Service for push notifications

- **C. Performance Considerations**
  - Efficient RecyclerViews for lists
  - Lazy loading content where possible
  - Minimal network calls
  - Caching considerations for future updates
  - Notifications optimized for low-latency delivery

- **D. Security Considerations**
  - Validation on login & registration screens
  - Controlled API access via Retrofit
  - Safe parameter handling
  - Secure storage of session data
  - Sanitized user inputs

- **E. Scalability**
  - Modular structure to easily add new features
  - Extendable models for future API expansion
  - Flexible UI components
  - Firebase integration capable of supporting thousands of users
  - The overall goal is to ensure the app can evolve smoothly as university needs grow.

### 3. Utilisation of GitHub

GitHub serves as the central development platform for UniVerse, providing:

- **A. Version Control**
  - All source code is tracked using Git
  - Every update is stored with commit history
  - Repository strategy allowed:
  - Progress feature implementation
  - Main Repository (Provided by VCPTA)

- **B. Collaboration**
  - GitHub enables:
    - Multiple developers contributing simultaneously
    - Pull requests for code review
    - Issue tracking for bugs, features, and enhancements

- **C. Documentation**
  - GitHub hosts:
    - README
    - API usage guides
    - Project roadmap
    - Change logs
    - Developer instructions
    - This ensures proper onboarding for new contributors.

### 4. Utilisation of GitHub Actions (CI/CD)

GitHub Actions is used to automate various development workflows:

- **A. Automated Build Pipeline**
  - Workflows configured to:
    - Automatically build the Android project on every push
    - Validate that the project compiles without errors
    - Prevent broken code from reaching the main branch

- **B. Security & Scanning**
  - Actions also:
    - Scan for vulnerabilities
    - Flag outdated dependencies
    - Verify secrets usage
    - GitHub Actions helps ensure that every commit is reliable, improving code quality and developer productivity.

- **C. SonarQube**
<p align="center">
  <img src="https://github.com/VCPTA/bca3-prog7314-poe-submission-ST10372590/blob/main/SonarQube.png" width="1000" />
</p>

## Application Overview

### Key Features

**1. Authentication**
- Register using name, email, password, and role
- Login with secure session handling
- Biometric login with Fingerprint or Facial Recognition
- Login with SSO
- Form validation
- API communication via Retrofit

**2. Dashboards**

- Dedicated dashboards for:
  - **Students:**
    - View modules
    - Access content
    - Read announcements
    - Check messages
    - View leaderboard

  - **Lecturers:**
    - Manage modules
    - Post announcements
    - Review submissions
    - View analytics

**3. Announcements System**
- Create announcements
- View announcement lists
- Receive push notifications when new announcements arrive

**4. Modules & Academic Content**
- Explore modules
- View module resources
- Navigate module-specific announcements

**5. Messaging System**
- Real-time chat UI
- Lecturer–student messaging
- Recyclerview with fast rendering

**6. Leaderboard System**
- Ranked list of student scores or participation
- Dynamic ranking updates
- Students are on leaderboard based on the points they gain when they engage in gaming etc

**7. Lecturer Submission & Review**
- View pending submissions
- Review and provide feedback
- Send feedback ad grading to the students

**8. Push Notifications**
Powered by Firebase Cloud Messaging
- **Alerts for:**
  - announcements
  - submission changes
  - system messages

Network Infrastructure
- **Managed via:**
  - ApiClient.kt (Retrofit)
  - Models for JSON requests
  - NetworkUtils for connectivity checks
 
### Innovative Features added in this version

- **Smooth File Sharing & Collaboration:**
  - This feature makes it easy to upload, download, and distribute files, including projects, assignments, and lecture notes.
  - Allows for both local and cloud storage, which facilitates effective and convenient student-lecturer communication

- **Integrated Calendar & Deadlines:**
  - This feature unifies all academic due dates, including assignments, exams, and events, into a single calendar. 
  - Synchronises automatically with assignments made by teachers, guaranteeing that pupils never overlook deadlines and enhancing time management

- **Progress tracking and analytics:**
  - This tool gives students access to graphic dashboards that show their grades, trends, and streaks.
  - To enhance learning outcomes, lecturers can track student performance, spot troublesome students, and use data-driven interventions 

### Tech Stack
- **Languages & Tools**
  - Kotlin
  - Android Studio
  - XML layouts
  - Gradle
  - Libraries
  - Retrofit
  - Gson
  - Firebase Cloud Messaging
  - RecyclerView
  - ViewBinding

### Building the Project
**Requirements**
- Android Studio Dolphin+
- Minimum SDK: 21
- Target SDK: 34

**Steps**
- Open Android Studio
- Load the project
- Let Gradle sync
- Run on emulator/device
