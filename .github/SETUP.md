# GitHub Actions Setup Guide

This guide will help you configure GitHub Actions for automated testing.

## Prerequisites

- GitHub repository with this code
- Google Cloud Platform account
- GCS bucket created for testing

## Step-by-Step Setup

### 1. Create a Service Account

```bash
# Set your project ID
export PROJECT_ID="your-project-id"

# Create service account
gcloud iam service-accounts create gcs-testing-sa \
  --display-name="GCS Testing Service Account" \
  --project=$PROJECT_ID

# Grant Storage Admin role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:gcs-testing-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/storage.admin"

# Create and download key
gcloud iam service-accounts keys create ~/gcs-testing-key.json \
  --iam-account=gcs-testing-sa@${PROJECT_ID}.iam.gserviceaccount.com
```

### 2. Configure GitHub Secrets

1. Go to your GitHub repository
2. Navigate to: **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"**
4. Add the following secrets:

#### Secret: GCS_PROJECT_ID
- **Name:** `GCS_PROJECT_ID`
- **Value:** Your Google Cloud project ID (e.g., `gcs-cli-testing`)

#### Secret: GCS_BUCKET_NAME
- **Name:** `GCS_BUCKET_NAME`
- **Value:** Your GCS bucket name (e.g., `gcs-test-bucket-antonmalinovski-1761653193`)

#### Secret: GCP_SA_KEY
- **Name:** `GCP_SA_KEY`
- **Value:** The entire contents of `~/gcs-testing-key.json`

To get the JSON content:
```bash
cat ~/gcs-testing-key.json
```

Copy the **entire output** (from `{` to `}`) and paste it as the secret value.

### 3. Verify Setup

1. Push code to `main` or `develop` branch:
   ```bash
   git add .
   git commit -m "Add GitHub Actions CI/CD"
   git push origin main
   ```

2. Go to **Actions** tab in your GitHub repository
3. You should see the workflow running
4. Click on the workflow run to see progress

### 4. Test Manual Trigger

1. Go to **Actions** tab
2. Select **"GCS CLI Testing CI/CD"** workflow
3. Click **"Run workflow"** button
4. Select branch and click **"Run workflow"**

## Workflow Triggers

### Main CI/CD Workflow (`ci.yml`)
Runs automatically on:
- ✅ Push to `main` branch
- ✅ Push to `develop` branch
- ✅ Pull requests to `main` or `develop`
- ✅ Manual trigger (workflow_dispatch)

### Scheduled Tests (`scheduled-tests.yml`)
Runs automatically:
- ✅ Daily at 9:00 AM UTC
- ✅ Manual trigger (workflow_dispatch)

## Troubleshooting

### Error: "Authentication failed"
- Check that `GCP_SA_KEY` secret contains valid JSON
- Verify service account has Storage Admin role
- Ensure project ID matches

### Error: "Bucket not found"
- Verify `GCS_BUCKET_NAME` secret is correct
- Check bucket exists: `gcloud storage ls`
- Ensure service account has access to bucket

### Error: "Playwright browsers not found"
- This is handled automatically in the workflow
- Browsers are installed during workflow run

### Tests failing locally but passing in CI
- Check environment variables are set correctly locally
- Verify you're using the same Java version (11)
- Ensure gcloud authentication is configured locally

## Security Notes

- ⚠️ Never commit `gcs-testing-key.json` to git
- ⚠️ Add `*.json` to `.gitignore` for credential files
- ✅ Service account keys are stored securely in GitHub Secrets
- ✅ Test files are automatically cleaned up after each run

## Viewing Test Results

### In Pull Requests
- Test results are automatically commented on PRs
- Shows pass/fail counts and status

### In Actions Tab
1. Go to **Actions** → Select workflow run
2. View test summary in the run overview
3. Download artifacts for detailed reports:
   - `test-reports` - HTML and XML reports
   - `test-results` - XML test results

### Screenshots
If tests fail, screenshots are available in the `test-reports` artifact under `target/screenshots/`

## Cleanup

To remove the service account later:
```bash
# Delete service account
gcloud iam service-accounts delete \
  gcs-testing-sa@${PROJECT_ID}.iam.gserviceaccount.com \
  --project=$PROJECT_ID

# Remove local key file
rm ~/gcs-testing-key.json
```

## Next Steps

- ✅ Configure branch protection rules to require passing tests
- ✅ Set up Slack/email notifications for test failures
- ✅ Review scheduled test results daily
- ✅ Monitor for phishing warnings in test reports
