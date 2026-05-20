# Security Policy

This project is public but entirely untested.

## Reporting a Vulnerability

Do not post secrets, OAuth tokens, client credentials, keystore files, passwords, private Drive links, or personal data in public issues, pull requests, commits, or screenshots.

If you find a security problem, contact the maintainer privately before sharing details publicly. Include only the minimum information needed to understand the issue.

## Sensitive Files

Keep these out of source control:

- `local.properties`
- `oauth.local.properties`
- signing keystores such as `*.jks` and `*.keystore`
- APK/AAB build outputs
- OAuth tokens, refresh tokens, and real client secrets

The repository includes placeholder OAuth configuration only. Real Google OAuth client IDs should be supplied locally through environment variables, Gradle properties, or ignored local property files.
