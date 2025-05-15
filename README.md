# NotBroke - Personal Finance Management App

NotBroke is a comprehensive personal finance management application designed to help users track their expenses, manage budgets, and improve their financial habits. Built with modern Android development practices, it offers a user-friendly interface and powerful features to help users achieve their financial goals.
### GitHub Links:
https://github.com/VCSTDN2024/prog7313-part2-dreamteam/tree/main
https://github.com/Max-Thunderbolt/NotBroke/tree/master 
### Demo Video
https://youtu.be/Rl4SlB1U8-I

## Features

### Authentication & User Management
- Secure email/password authentication
- Google Sign-In integration
- User profile management
- Session persistence

### Financial Management
- **Dashboard**: Overview of financial status
- **Expense Tracking**: Categorize and monitor expenses
- **Budget Management**: Set and track budget goals
- **Debt Tracking**: Monitor and manage debts
- **Net Worth Calculator**: Track overall financial health
- **Financial Progression**: Visualize financial growth over time

### Habit Building
- Financial habit tracking
- Goal setting and progress monitoring
- Customizable categories for expenses

### Data Management
- Cloud synchronization with Firebase
- Offline data persistence
- Secure data storage
- Automatic categorization of transactions

## Technical Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth
- **UI**: Material Design Components
- **Dependency Injection**: Manual DI
- **Coroutines**: For asynchronous operations
- **LiveData**: For reactive UI updates

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/notbroke/
│   │   │   ├── activities/     # Main app screens
│   │   │   ├── fragments/      # UI components
│   │   │   ├── models/         # Data models
│   │   │   ├── repositories/   # Data access layer
│   │   │   ├── services/       # Business logic
│   │   │   ├── utils/          # Helper classes
│   │   │   └── DAO/            # Data Access Objects
│   │   └── res/                # Resources
└── build.gradle                # App-level build configuration
```

## Getting Started

### Prerequisites
- Android Studio (latest version)
- Android SDK (API level 24 or higher)
- Google Firebase account
- Google Cloud Console account (for Google Sign-In)

### Installation
1. Clone the repository
2. Open the project in Android Studio
3. Add your `google-services.json` file to the app directory
4. Update the Google Sign-In client ID in `MainActivity.kt`
5. Sync the project with Gradle files
6. Run the app on an emulator or physical device

### Configuration
1. Set up Firebase project
2. Enable Authentication methods (Email/Password and Google Sign-In)
3. Configure Firestore database
4. Update security rules as needed

## Usage

1. **Registration & Login**
   - Create an account using email/password or Google Sign-In
   - Log in to access your financial dashboard

2. **Dashboard**
   - View financial overview
   - Access quick actions
   - Monitor key metrics

3. **Expense Management**
   - Add new expenses
   - Categorize transactions
   - Set budget limits
   - View expense history

4. **Financial Goals**
   - Set savings targets
   - Track debt repayment
   - Monitor net worth
   - View financial progression

5. **Habit Tracking**
   - Set financial habits
   - Track progress
   - Receive reminders
   - View habit statistics

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Firebase for backend services
- Google Material Design for UI components
- Android Jetpack for architecture components
- Kotlin Coroutines for asynchronous programming

## Support

For support, please open an issue in the GitHub repository or contact the development team.

## Roadmap

- [ ] Enhanced data visualization
- [ ] Investment tracking
- [ ] Bill payment reminders
- [ ] Export/Import functionality
- [ ] Multi-currency support
- [ ] Family budget sharing
- [ ] AI-powered expense categorization
- [ ] Financial insights and recommendations
