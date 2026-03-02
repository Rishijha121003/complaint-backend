#!/bin/bash
echo -n "Enter your live backend URL (e.g., https://my-backend.onrender.com): "
read NEW_URL

if [ -z "$NEW_URL" ]; then
    echo "Error: New URL cannot be empty."
    exit 1
fi

OLD_URL="http://localhost:8080"
echo "Replacing '$OLD_URL' with '$NEW_URL' in all HTML files..."

find /home/rishi/Downloads/Complaint-frontend/Complain-frontend/pages -name "*.html" -type f -exec sed -i "s|$OLD_URL|$NEW_URL|g" {} +

echo "✅ Deployment URLs updated successfully!"
