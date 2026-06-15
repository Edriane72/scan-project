# SCAN Prototype

## Installation / Setup Process

1. Install Android Studio and JDK 17 or newer.
2. Launch Android Studio and complete the initial setup to install the required Android SDK components.
3. Download or clone the project repository from GitHub.
4. Open the project folder in Android Studio.
5. Allow Gradle to synchronize and automatically download the required dependencies and build tools.
6. Launch an Android Emulator or connect a physical Android device with USB/Wireless Debugging enabled.
7. Build and run the application through Android Studio.
8. Enter the required simulation information.
9. Press **SIMULATE CALL** to execute the call screening process and view the generated verdict, details, audit log, and Call HUD output.

---

# Project Overview on Features

## From the Dashboard

- **Caller Number (Text Field)** - Allows the user to enter the incoming caller’s phone number. This number is compared with the User SIM Number and with the blacklisted and whitelisted numbers.

- **User SIM Number (Text Field)** - Allows the user to enter the user’s SIM phone number. This is what the Caller Number is compared to when examining prefixes to detect neighbor spoofing.

- **CNAM (Text Field)** - Allows the user to set a custom display name for identification purposes.

- **Call Hour (Text Field)** - Allow the user to specify the time the call occurred. When empty, the system uses the device’s time. Contributes to the heuristic score.

- **Enable Call HUD (Button)** - Enables the simulation of an incoming call, pop-up warnings, block pop-up, and the in-call HUD.

- **Advanced Heuristic Settings (Button)** - Opens additional settings that allow users to customize how calls are evaluated.

  - **Time Anomaly Weight** - Adjust the importance of calls during unusual hours.
  - **Call Velocity Weight** - Adjust the importance of repeated calls from the same number within a short period.
  - **Neighbor Spoof Weight** - Adjust the importance of calls coming from numbers that closely resemble the user's own number.
  - **WARN Threshold** - Set a custom minimum risk score required to result in the WARN verdict.
  - **BLOCK Threshold** - Set a custom minimum risk score required to result in the BLOCK verdict.

- **SIMULATE CALL (Button)** - Button to evaluate the values and initiate the call.

- **Verdict (Output)** - Displays the verdict from the heuristics along with the scores.

- **Details (Output)** - Display the reason and indicators why the verdict was called.

- **Audit Log (Output)** - Displays a record of the screening process and the actions performed during the call analysis.

---

# General Backend Feature

## 1. Normalization

- The program automatically normalizes the format of phone numbers to follow the format of `639*********`.
- Automatically converts numbers with formats such as:
  - `+639*********`
  - `09**-***-****`
  - `09** *** ****`

## 2. Input and Whitelist Validation

- Numbers that are too short/long are normalized and then checked before proceeding with the calls. After checking, it determines whether it follows the normalized number format. Stops the run when invalidated.
- Whitelisted numbers are allowed to proceed even when they don't match the determined format, to simulate allowing numbers from hotlines or government numbers that don’t follow the usual format.

## 3. Blacklist Verification

- Compares the incoming calls from numbers against a pre-determined list.
- Applies the Boyer-Moore algorithm to determine whether incoming calls match the blacklisted numbers.

## 4. Time-Based Analysis

- Consider the time of day when the call was made, contributing to the overall heuristic scoring to determine the verdict of the call.

## 5. Call Frequency Analysis

- Consider the frequency of the call from the same number within a 24-hour period, contributing to the overall heuristics scoring to determine the verdict of the call.

## 6. Neighbor Spoofing Analysis

- Applies normal prefix matching to determine if the incoming calls resemble the user’s number, contributing to the overall heuristic scoring to determine the verdict of the call.

## 7. Risk Scoring System

- Assigns a score based on detected indicators to measure the likelihood that a call is suspicious or needs to be blocked.