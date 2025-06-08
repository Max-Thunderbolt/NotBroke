# NotBroke

NotBroke is a personal finance management Android application designed to help users track their income, expenses, debts, net worth, and financial habits. The app provides visual insights, budgeting tools, debt payoff strategies, and rewards for good financial behavior. Data is securely stored and synchronized using Firebase and Room database for offline support.

## Repositories 
- Main: https://github.com/Max-Thunderbolt/NotBroke
- Part 2: https://github.com/VCSTDN2024/prog7313-part2-dreamteam

## Demo Video
- https://youtu.be/Rl4SlB1U8-I

## Features

- **User Authentication**
  - Sign up and log in with email/password or Google account (via Firebase Auth).
  - Secure user data separation.

- **Transaction Management**
  - Add, edit, and delete income and expense transactions.
  - Attach receipt images to transactions.
  - Categorize transactions automatically or manually.

- **Budgeting**
  - Set monthly spending limits per category.
  - Visualize budget progress and receive color-coded feedback.
  - View summaries of total budget, spent, and remaining amounts.

- **Debt Tracking & Strategies**
  - Add and manage multiple debts (loans, credit cards, etc.).
  - Track payments, interest, and payoff progress.
  - Apply various debt payoff strategies (Avalanche, Snowball, etc.).
  - Visualize debt payoff timeline and progress.

- **Net Worth Tracking**
  - Add assets and liabilities.
  - Track net worth over time with historical entries.
  - View net worth trends and summaries.

- **Rewards & Progression**
  - Earn rewards for meeting financial goals (e.g., staying within budget, saving, etc.).
  - Claim and view rewards in a dedicated progression section.

- **Financial Habits & Insights**
  - Visualize spending and saving habits with charts (MPAndroidChart).
  - Compare spending across months and categories.
  - Analyze trends and set personal goals.

- **Data Synchronization & Offline Support**
  - All data is stored in Firebase Firestore for cloud sync.
  - Local data is cached using Room database for offline access.
  - Automatic sync between local and cloud data.

## Technologies Used

- **Kotlin** (primary language)
- **Android Jetpack** (Room, ViewModel, LiveData, etc.)
- **Firebase** (Auth, Firestore)
- **Kotlin Coroutines & Flow** (for async and real-time updates)
- **MPAndroidChart** (for data visualization)
- **Material Design Components**

## Project Structure
```
NotBroke/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/notbroke/
│   │   │   │   ├── adapters/         # RecyclerView adapters for displaying lists (transactions, debts, rewards, etc.)
│   │   │   │   ├── DAO/              # Room database entities and Data Access Objects for local storage
│   │   │   │   ├── fragments/        # UI Fragments for each main screen (Dashboard, Debt, Net Worth, Settings, etc.)
│   │   │   │   ├── models/           # Data models (Transaction, Debt, Category, UserProfile, etc.)
│   │   │   │   ├── repositories/     # Data repositories for managing data flow between Room, Firestore, and UI
│   │   │   │   ├── services/         # Firebase, authentication, and sync services
│   │   │   │   ├── utils/            # Utility classes (e.g., categorization, helpers)
│   │   │   │   ├── NotBrokeApplication.kt # Application class for global initialization
│   │   │   │   ├── HomeActivity.kt        # Main activity with navigation drawer
│   │   │   │   ├── MainActivity.kt        # Login and authentication logic
│   │   │   │   ├── RegisterActivity.kt    # User registration screen
│   │   │   │   ├── ProfileActivity.kt     # User profile management
│   │   │   │   ├── TestData.kt            # Sample/test data for development
│   │   │   ├── res/
│   │   │   │   ├── layout/            # XML layout files for activities, fragments, dialogs, and list items
│   │   │   │   ├── drawable/          # App icons, backgrounds, and vector assets
│   │   │   │   ├── values/            # Colors, strings, styles, and themes
│   │   │   │   ├── menu/              # Menu XMLs for navigation and actions
│   │   │   │   ├── mipmap-*/          # Launcher icons for various screen densities
│   │   │   │   ├── xml/               # Miscellaneous XML resources (e.g., file provider)
│   │   ├── test/                      # Unit and instrumentation tests
│   │   ├── androidTest/               # UI tests
│   ├── build.gradle                   # App-level Gradle configuration
├── gradle/                            # Gradle wrapper and configuration
├── build.gradle.kts                   # Project-level Gradle configuration
├── settings.gradle.kts                 # Gradle settings
├── google-services.json                # Firebase configuration (not included in repo)
├── README.md                           # Project documentation
└── ...                                 # Other configuration and metadata files
```

## Getting Started

### Prerequisites
- **Android Studio** (latest stable version recommended)
- **Firebase Project**
  - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
  - Enable Email/Password and Google authentication in Firebase Auth
  - Download the `google-services.json` file and place it in the `app/` directory

### Installation
1. **Clone the repository:**
   ```
   git clone https://github.com/Max-Thunderbolt/NotBroke.git
   ```
2. **Open in Android Studio:**
   - Open the project folder in Android Studio.
3. **Add Firebase configuration:**
   - Place your `google-services.json` file in the `app/` directory.
4. **Sync Gradle:**
   - Let Android Studio sync and download dependencies.
5. **Build and Run:**
   - Connect an Android device or start an emulator.
   - Click **Run** to build and launch the app.

### Usage
- **Register** a new account or log in with an existing one.
- **Add transactions** (income/expense) and attach receipts if desired.
- **Set budgets** for categories in the Settings section.
- **Track debts** and make payments using different strategies.
- **Monitor net worth** by adding assets and liabilities.
- **Earn rewards** for good financial habits and view your progression.
- **Visualize** your financial data with interactive charts and summaries.

## Notes
- The app uses both local (Room) and cloud (Firestore) storage for reliability and offline support.
- All sensitive operations (like authentication) are handled securely via Firebase.
- The app is intended for educational and personal use.

## License
This project is for educational purposes as part of a university assignment. All rights reserved to the project authors and contributors.

---

If you have any questions or issues, please contact the project maintainer or your course instructor.

