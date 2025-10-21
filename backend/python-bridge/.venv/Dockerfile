# Use official Python base image
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies (if needed — optional)
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy your app code into the container
COPY . .
COPY .env .env

# Environment variables (optional fallback — overridden by docker-compose)
ENV PYTHONUNBUFFERED=1

# Set the default command to run your app
CMD ["python", "app.py"]