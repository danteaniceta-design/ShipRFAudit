# Ship RF Audit

**Ship RF Audit** is a small Android Wi-Fi survey app, accompanied by a responsive website with a **direct APK download button** and a **local browser observation logger**.

## Download for Android

- [Download Android APK (direct from GitHub repository)](https://raw.githubusercontent.com/danteaniceta-design/ShipRFAudit/main/downloads/ShipRFAudit.apk)
- [Alternative: GitHub Releases](https://github.com/danteaniceta-design/ShipRFAudit/releases/tag/v1.0.0)

On Android, open the APK file from **Files → Downloads** and install it if your device's security policy allows sideloading. The APK is debug-signed and not published through Google Play. For Android 8+.

## Website

Website code is in `docs/`. To make it accessible at **https://danteaniceta-design.github.io/ShipRFAudit/**, enable GitHub Pages:

1. Visit **Settings → Pages** in this repository: https://github.com/danteaniceta-design/ShipRFAudit/settings/pages
2. Under **Build and deployment**, select **Deploy from a branch**.
3. Set **Branch: main** and **Folder: /docs**, then **Save**.
4. GitHub publishes the website after deployment finishes.

The browser tool can **manually log** SSIDs, signal readings, and areas, then export a local CSV. **Browser JavaScript cannot scan nearby Wi-Fi networks.** Native Android scanning requires the downloaded APK and Wi-Fi/Location permissions.

## Android app

- Scan nearby Wi-Fi SSIDs, BSSIDs, frequencies and RSSI on devices that permit it.
- Flag SSIDs that include the string "Starlink"; these flags are not proof of device type.
- Save local observations with survey area and timestamp; export CSV.
- No background traffic interception or identifying people.

**Important:** An independent Starlink terminal with Wi-Fi switched off or a renamed SSID may not be identifiable. Use this tool only for authorized surveys and coordinate investigations with IT management and onboard Security.
