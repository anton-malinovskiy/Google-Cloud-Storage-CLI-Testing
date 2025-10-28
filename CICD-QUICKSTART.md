# CI/CD Quick Start Guide

## Quick Setup (5 minutes)

### 1. Create Service Account & Get Credentials

```bash
# Set your project ID
export PROJECT_ID="your-project-id"

# Create service account
gcloud iam service-accounts create gcs-testing-sa \
  --display-name="GCS Testing Service Account" \
  --project=$PROJECT_ID

# Grant permissions
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:gcs-testing-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/storage.admin"

# Download credentials
gcloud iam service-accounts keys create ~/gcs-testing-key.json \
  --iam-account=gcs-testing-sa@${PROJECT_ID}.iam.gserviceaccount.com

# Display credentials (copy this output)
cat ~/gcs-testing-key.json
```

### 2. Configure GitHub Secrets

Go to: **GitHub Repo** → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Add these 3 secrets:

| Secret Name | Value | Where to Get It |
|-------------|-------|-----------------|
| `GCS_PROJECT_ID` | `your-project-id` | Your GCP project ID |
| `GCS_BUCKET_NAME` | `your-bucket-name` | Your test bucket name |
| `GCP_SA_KEY` | Entire JSON from step 1 | Output of `cat ~/gcs-testing-key.json` |

### 3. Update README Badges

In `README.md`, replace `YOUR_USERNAME` and `YOUR_REPO_NAME` with your actual GitHub username and repository name.

### 4. Push to GitHub

```bash
git add .
git commit -m "Add GitHub Actions CI/CD pipeline"
git push origin main
```

### 5. Verify It's Working

1. Go to **Actions** tab in GitHub
2. You should see workflow running
3. Wait for it to complete
4. Check for green checkmark ✅

## What Happens Now?

### Automatic Triggers

✅ **On every push to `main` or `develop`:**
- All tests run automatically
- Test reports generated
- Test files cleaned up

✅ **On every pull request:**
- Tests run on PR code
- Results commented on PR
- Must pass before merge

✅ **Daily at 9 AM UTC:**
- Phishing detection tests run
- Issue created if phishing detected
- Monitors for GCS changes

### Manual Trigger

1. Go to **Actions** tab
2. Click workflow name
3. Click **"Run workflow"**
4. Select branch → **"Run workflow"**

## Viewing Results

### Test Summary
- Go to **Actions** → Click workflow run
- See pass/fail counts
- View duration and status

### Detailed Reports
- Click workflow run
- Scroll to **Artifacts** section
- Download `test-reports` or `test-results`
- Open HTML files in browser

### Screenshots (if tests fail)
- Download `test-reports` artifact
- Extract ZIP
- Open `target/screenshots/*.png`

## Troubleshooting

### "Authentication failed"
```bash
# Verify service account exists
gcloud iam service-accounts list --project=$PROJECT_ID

# Check permissions
gcloud projects get-iam-policy $PROJECT_ID \
  --flatten="bindings[].members" \
  --filter="bindings.members:gcs-testing-sa@*"
```

### "Bucket not found"
```bash
# Verify bucket exists
gcloud storage ls --project=$PROJECT_ID

# Check bucket permissions
gcloud storage buckets describe gs://YOUR_BUCKET_NAME
```

### "Tests pass locally but fail in CI"
- Check environment variable names match exactly
- Verify Java version (should be 11)
- Ensure service account has correct permissions

## Security Checklist

- [ ] Never commit `gcs-testing-key.json` to git
- [ ] Verify `.gitignore` includes `*.json`
- [ ] Service account has minimum required permissions (Storage Admin only)
- [ ] GitHub secrets are configured as "Repository secrets" (not environment)
- [ ] Delete local key file after setup: `rm ~/gcs-testing-key.json`

## Customization

### Change scheduled test time
Edit `.github/workflows/scheduled-tests.yml`:
```yaml
schedule:
  - cron: '0 9 * * *'  # Change to your preferred time (UTC)
```

### Add more branches to CI
Edit `.github/workflows/ci.yml`:
```yaml
on:
  push:
    branches:
      - main
      - develop
      - feature/*  # Add this
```

### Run only specific tests
Edit `.github/workflows/ci.yml`, change:
```yaml
- name: Run tests
  run: mvn test -Dtest=SignUrlTest  # Run only SignUrlTest
```

## Full Documentation

- Detailed setup: [.github/SETUP.md](.github/SETUP.md)
- Main README: [README.md](README.md)
- Workflow files: `.github/workflows/`

## Need Help?

1. Check workflow logs in Actions tab
2. Review [.github/SETUP.md](.github/SETUP.md) for detailed troubleshooting
3. Verify all secrets are configured correctly
4. Check service account has correct permissions

---

Your CI/CD pipeline is now configured. Every push will trigger automated tests.
