# Security Notice - Firebase Credentials

## ⚠️ CRITICAL: Firebase Service Account Key Rotation Required

**A Firebase service account private key was previously committed to this repository.**

### Immediate Actions Required:

1. **Rotate the Firebase Service Account Key:**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Select your project: `server-check-64d4d`
   - Navigate to: Project Settings → Service Accounts
   - Click "Generate New Private Key"
   - Download the new JSON file
   - Delete the old key from Firebase Console

2. **Update Your Environment:**
   - Save the new credentials JSON file securely (DO NOT commit it)
   - For Replit: Store in Secrets tab as `FIREBASE_CREDENTIALS_JSON`
   - For local: Save to `storage/app/firebase-credentials.json` (already in .gitignore)
   - Update `.env`: `FIREBASE_CREDENTIALS=storage/app/firebase-credentials.json`

3. **Verify .gitignore:**
   - Ensure `storage/app/firebase-credentials.json` is in `.gitignore`
   - Never commit Firebase credentials to version control

### Affected Credentials:
- **Project ID:** server-check-64d4d
- **Service Account:** firebase-adminsdk-fbsvc@server-check-64d4d.iam.gserviceaccount.com
- **Private Key ID:** bfc52fb6a7ab958c03534c386647fff051bad56a
- **Status:** ⚠️ COMPROMISED - Must be rotated immediately

### Security Best Practices:

✅ **DO:**
- Store credentials in environment variables or secure secrets management
- Add credential files to `.gitignore` before committing
- Rotate keys immediately if they are exposed
- Use Replit Secrets for sensitive data in production

❌ **DON'T:**
- Commit credentials to git repositories
- Share credential files via insecure channels
- Use the same credentials across multiple environments
- Store credentials in code or configuration files

### Verification:

After rotating the key, verify it works:
```bash
php artisan tinker
>>> app(\App\Services\FirebaseService::class);
# Should load without errors
```

---
**Last Updated:** October 30, 2025  
**Status:** Awaiting key rotation
