import requests
import json

# Test the dashboard-stats API
url = "http://192.168.0.223:5051/dashboard-stats"
params = {
    "date": "2026-04-23",
    "branch_id": 29
}

print(f"Testing API: {url}")
print(f"Parameters: {params}")
print("-" * 60)

try:
    response = requests.get(url, params=params)
    print(f"Status Code: {response.status_code}")
    print(f"Response:")
    print(json.dumps(response.json(), indent=2))
except Exception as e:
    print(f"Error: {e}")
    print(f"Raw Response: {response.text if 'response' in locals() else 'No response'}")

