# Google Cloud Storage CLI Testing Framework

Automated testing framework for Google Cloud Storage CLI (gcloud storage) commands with a focus on detecting phishing warnings when accessing signed URLs.

## Overview

This framework tests 4 essential GCS CLI commands:
1. **sign-url** (PRIMARY FOCUS) - Generates signed URLs and validates them for phishing warnings
2. **cp** - Copy files to/from GCS buckets
3. **ls** - List bucket contents
4. **rm** - Delete files from buckets

## Key Features

- **Phishing Detection**: Uses Playwright to detect browser phishing warnings on signed URLs
- **Test Independence**: Each test creates unique test data using UUIDs
- **Parallel Execution**: Tests can run concurrently with thread-safe implementations
- **Test Coverage**: Multiple test scenarios for each command
- **Docker Support**: Ready for CI/CD pipelines
- **Detailed Reporting**: HTML and XML test reports

## Prerequisites

- **Java 11+**
- **Maven 3.6+**
- **Google Cloud SDK** (gcloud CLI)
- **Google Cloud Account** with a GCS bucket
- **Authentication**: Either gcloud auth login or service account

## Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd gcs-cli-testing
   ```

2. **Set up Google Cloud Authentication**
   ```bash
   # Option 1: User authentication
   gcloud auth login
   gcloud config set project YOUR_PROJECT_ID

   # Option 2: Service account (set in .env)
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
   ```

3. **Create a test bucket** (if not existing)
   ```bash
   gsutil mb gs://your-test-bucket-name
   ```

4. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your values:
   # GCS_PROJECT_ID=your-project-id
   # GCS_BUCKET_NAME=your-test-bucket
   ```

5. **Install Playwright browsers** (first time only)
   ```bash
   mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
   ```

6. **Run all tests**
   ```bash
   export GCS_PROJECT_ID=your-project-id
   export GCS_BUCKET_NAME=your-test-bucket
   mvn test
   ```

## Running Tests

### Run all tests
```bash
mvn test
```

### Run specific test class
```bash
mvn test -Dtest=SignUrlTest
```

### Run specific test method
```bash
mvn test -Dtest=SignUrlTest#testSignUrlWithTextFile
```

### Run with custom configuration
```bash
mvn test -Dgcs.project.id=my-project -Dgcs.bucket.name=my-bucket
```

## CI/CD Pipeline (GitHub Actions)

This project includes automated CI/CD pipelines that run on every push to `main` or `develop` branches.

### Setup GitHub Actions

1. **Configure GitHub Secrets** (required for CI/CD to work):

   Go to your repository → Settings → Secrets and variables → Actions, then add:

   | Secret Name | Description | Example |
   |-------------|-------------|---------|
   | `GCS_PROJECT_ID` | Your Google Cloud Project ID | `gcs-cli-testing` |
   | `GCS_BUCKET_NAME` | GCS bucket for testing | `gcs-test-bucket-name` |
   | `GCP_SA_KEY` | Service account JSON key | `{"type": "service_account"...}` |

2. **Create Service Account** (if you don't have one):

   ```bash
   # Create service account
   gcloud iam service-accounts create gcs-testing-sa \
     --display-name="GCS Testing Service Account"

   # Grant Storage Admin role
   gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
     --member="serviceAccount:gcs-testing-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
     --role="roles/storage.admin"

   # Create and download key
   gcloud iam service-accounts keys create ~/gcs-testing-key.json \
     --iam-account=gcs-testing-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com

   # Copy the entire JSON content and paste it as GCP_SA_KEY secret
   cat ~/gcs-testing-key.json
   ```

3. **Workflows**:

   - **`ci.yml`** - Runs on every push/PR to main/develop
     - Executes all tests
     - Uploads test reports as artifacts
     - Comments test results on PRs
     - Auto-cleans up test files

   - **`scheduled-tests.yml`** - Runs daily at 9 AM UTC
     - Monitors for phishing warnings
     - Creates GitHub issues on failure
     - Provides early warning of GCS changes

4. **Manual Trigger**:

   You can manually trigger workflows from the GitHub Actions tab → Select workflow → "Run workflow"

### CI/CD Features

- ✅ Automated testing on push and PRs
- ✅ Test result comments on PRs
- ✅ Test reports as downloadable artifacts
- ✅ Daily scheduled phishing detection checks
- ✅ Automatic test file cleanup
- ✅ Issue creation on critical failures
- ✅ Service account authentication
- ✅ Playwright browser installation

### Viewing Test Results

1. Go to **Actions** tab in your GitHub repository
2. Select the workflow run
3. View test results in the summary
4. Download artifacts for detailed reports and screenshots

## Docker Usage (Optional)

Docker support is available but **optional**. GitHub Actions is the recommended approach for CI/CD.

### Build and run with Docker
```bash
# Build the image
docker build -t gcs-cli-testing .

# Run tests
docker run --rm \
  -e GCS_PROJECT_ID=your-project-id \
  -e GCS_BUCKET_NAME=your-test-bucket \
  -v ~/.config/gcloud:/root/.config/gcloud:ro \
  -v $(pwd)/target:/app/target \
  gcs-cli-testing
```

### Using Docker Compose
```bash
# Run all tests
docker-compose up

# Run only sign-url tests
docker-compose --profile signurl-only up gcs-sign-url-test
```

**Note:** Docker setup requires gcloud authentication from the host machine. For automated testing, use GitHub Actions instead.

## Test Structure

### Sign URL Test (Priority)
Tests signed URL generation with critical phishing detection:
- Text files
- JSON files
- HTML files (most likely to trigger warnings)
- Binary files
- Short duration URLs
- Multiple concurrent URLs

**Phishing Detection Indicators:**
- "Deceptive site ahead" text
- "Phishing" keywords
- Red warning screens
- HTTP status code validation

### Copy Command Test
- Local to GCS uploads
- GCS to local downloads
- GCS to GCS copying
- Recursive operations
- Large file handling
- Content type preservation

### List Command Test
- Empty bucket listing
- Single and multiple files
- Different file types
- Nested structures
- Performance with many files

### Delete Command Test
- Single file deletion
- Multiple file deletion
- Wildcard patterns
- Recursive deletion
- Force deletion
- Special characters handling

## Configuration

### Environment Variables

| Variable | Required | Description | Default |
|----------|----------|-------------|---------|
| GCS_PROJECT_ID | No* | Google Cloud Project ID | - |
| GCS_BUCKET_NAME | Yes | GCS bucket for testing | - |
| GCS_TEST_FILE_PREFIX | No | Prefix for test files | test- |
| GOOGLE_APPLICATION_CREDENTIALS | No | Path to service account key | - |

*Required for some operations

### TestNG Configuration

The `testng.xml` file controls:
- Test execution order
- Parallel execution settings (thread-count=4)
- Test groups and methods
- Report generation

## Project Structure

```
gcs-cli-testing/
├── src/
│   ├── main/java/com/gcs/testing/
│   │   ├── config/
│   │   │   └── TestConfig.java         # Configuration management
│   │   ├── utils/
│   │   │   ├── GCloudCliExecutor.java  # CLI command execution
│   │   │   ├── BucketHelper.java       # GCS operations
│   │   │   ├── TestDataGenerator.java  # Test data generation
│   │   │   └── BrowserHelper.java      # Playwright integration
│   │   └── models/
│   │       └── CommandResult.java      # Command results model
│   └── test/java/com/gcs/testing/
│       ├── base/
│       │   └── BaseTest.java           # Base test functionality
│       ├── tests/
│       │   ├── SignUrlTest.java        # Sign URL tests
│       │   ├── CopyCommandTest.java    # Copy command tests
│       │   ├── ListCommandTest.java    # List command tests
│       │   └── DeleteCommandTest.java  # Delete command tests
│       └── TestRunner.java             # Programmatic test runner
├── pom.xml                             # Maven configuration
├── testng.xml                          # TestNG configuration
├── Dockerfile                          # Docker configuration
├── docker-compose.yml                  # Docker Compose setup
├── .env.example                        # Environment template
└── README.md                           # This file
```

## Expected Test Output

Successful test execution shows:
```
[INFO] Running TestSuite
=== Starting GCS CLI Test Suite ===
Configuration validated. Project: my-project, Bucket: my-bucket
Starting test class: SignUrlTest
=== Testing sign-url command with text file ===
Validation result: SignedUrlValidationResult{url='https://...', httpStatus=200, phishing=false}
...
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
```

## Troubleshooting

### Common Issues

1. **Authentication Failed**
   ```
   Error: Not authenticated with gcloud
   Solution: Run 'gcloud auth login' or set GOOGLE_APPLICATION_CREDENTIALS
   ```

2. **Bucket Not Found**
   ```
   Error: GCS_BUCKET_NAME must be set
   Solution: Export GCS_BUCKET_NAME=your-bucket or add to .env
   ```

3. **Playwright Browser Issues**
   ```
   Error: Browser not found
   Solution: Run 'mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"'
   ```

4. **Permission Denied**
   ```
   Error: 403 Forbidden
   Solution: Ensure your account has Storage Admin role on the bucket
   ```

5. **Phishing Warning Detected**
   ```
   Error: CRITICAL: Phishing warning detected!
   Solution: This is the primary concern - investigate why GCS URLs trigger warnings
   ```

### Debug Mode

Enable verbose logging:
```bash
mvn test -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

### Test Reports

Find test reports in:
- `target/surefire-reports/` - XML reports
- `target/surefire-reports/html/` - HTML reports
- `target/screenshots/` - Screenshots from failed tests

## Performance Considerations

- Tests complete in < 5 minutes total
- Parallel execution with 4 threads by default
- Retry logic for flaky network operations
- Efficient test data cleanup after each test

## Security Notes

- No credentials are stored in the code
- Test data is prefixed for easy identification
- Automatic cleanup of test files
- Isolated test execution

## Known Limitations

1. Requires active internet connection
2. Some tests may be affected by GCS rate limits
3. Phishing detection depends on browser behavior
4. Docker mode requires gcloud auth volume mount

## Contributing

When adding new tests:
1. Extend `BaseTest` for common functionality
2. Use `TestDataGenerator` for unique test data
3. Track created resources for cleanup
4. Add comprehensive logging
5. Update `testng.xml` with new test methods

## License

This project is for testing purposes only. Use at your own risk.