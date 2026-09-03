import time
import requests
import urllib3
import xml.etree.ElementTree as ET


USERNAME = "jampani24"
PASSWORD = "#ROhITH@2007"


LOGIN_URL = "https://gateway.iisertvm.ac.in:8090/login.xml"
GATEWAY_URL = "https://gateway.iisertvm.ac.in:8090/httpclient.html"

CHECK_INTERVAL = 30       # Check every 30 seconds
TIMEOUT = 8               # Network timeout in seconds
LOGIN_RETRIES = 3         # Number of login attempts
RETRY_DELAY = 5           # Delay between login attempts

urllib3.disable_warnings(
    urllib3.exceptions.InsecureRequestWarning
)

def internet_available():

    try:

        response = requests.get(
            "https://www.google.com/generate_204",
            timeout=TIMEOUT,
            allow_redirects=False
        )

        return response.status_code == 204

    except requests.RequestException:

        return False


def gateway_available():

    try:

        response = requests.get(
            GATEWAY_URL,
            timeout=TIMEOUT,
            verify=False,
            allow_redirects=False
        )

        return response.status_code < 500

    except requests.RequestException:

        return False



def login():

    print("  Attempting IISER authentication...")

    headers = {
        "Accept": "*/*",
        "Content-Type": "application/x-www-form-urlencoded",
        "Origin": "https://gateway.iisertvm.ac.in:8090",
        "Referer": GATEWAY_URL,
    }

    session = requests.Session()

    for attempt in range(1, LOGIN_RETRIES + 1):

        data = {
            "mode": "191",
            "username": USERNAME,
            "password": PASSWORD,

            "a": str(int(time.time() * 1000)),

            "producttype": "0",
        }

        try:

            response = session.post(
                LOGIN_URL,
                data=data,
                headers=headers,
                timeout=TIMEOUT,
                verify=False
            )

            print(
                f"  Login attempt {attempt}: "
                f"HTTP {response.status_code}"
            )

            root = ET.fromstring(response.text)

            status = root.findtext("status")
            message = root.findtext("message")

            if status == "LIVE":

                if message:
                    message = message.replace(
                        "{username}",
                        USERNAME
                    )

                print("Authentication successful")

                if message:
                    print("  " + message)

                return True

            print("Authentication failed")

            if message:
                print("  Message:", message)

        except requests.RequestException as error:

            print("  Connection error:", error)

        except ET.ParseError:

            print("  ✗ Unexpected response from gateway")

            # Uncomment this if you need to debug the response:
            # print(response.text)

        # Retry
        if attempt < LOGIN_RETRIES:

            print(
                f"  Retrying in {RETRY_DELAY} seconds..."
            )

            time.sleep(RETRY_DELAY)

    return False


# ============================================================
# MAIN MONITOR
# ============================================================

def main():

    print("=" * 60)
    print("          IISER TVM AUTO AUTHENTICATION")
    print("=" * 60)

    print(f"Username: {USERNAME}")
    print("Network:  Wi-Fi / Ethernet")
    print(f"Check interval: {CHECK_INTERVAL} seconds")
    print()
    print("Auto-login monitor started.")
    print("Press Ctrl+C to stop.")
    print()

    while True:

        timestamp = time.strftime("%H:%M:%S")

        print(f"[{timestamp}] Checking network...")


        # ----------------------------------------------------
        # 1. Internet already works
        # ----------------------------------------------------

        if internet_available():

            print("  ✓ Internet available")
            print()

            time.sleep(CHECK_INTERVAL)
            continue


        # ----------------------------------------------------
        # 2. Internet doesn't work
        # ----------------------------------------------------

        print("  ✗ Internet unavailable")


        # ----------------------------------------------------
        # 3. Check whether IISER gateway is reachable
        # ----------------------------------------------------

        if not gateway_available():

            print("  ✗ IISER gateway not reachable")
            print("  Network may be disconnected")
            print()

            time.sleep(CHECK_INTERVAL)
            continue


        # ----------------------------------------------------
        # 4. Gateway reachable → attempt login
        # ----------------------------------------------------

        print("  ✓ IISER gateway detected")

        success = login()


        # ----------------------------------------------------
        # 5. Verify Internet after login
        # ----------------------------------------------------

        if success:

            print("  Checking Internet after authentication...")

            # Give Sophos a moment to update the firewall
            time.sleep(2)

            if internet_available():

                print("  ✓ Internet connection restored")

            else:

                print(
                    "  ⚠ Authentication succeeded, "
                    "but Internet is still unavailable."
                )

        else:

            print("  ✗ Automatic authentication failed")


        print()

        time.sleep(CHECK_INTERVAL)


# ============================================================
# START PROGRAM
# ============================================================

if __name__ == "__main__":

    try:

        main()

    except KeyboardInterrupt:

        print()
        print("IISER Auto Authentication stopped.")