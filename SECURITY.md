# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in Cadence, please report it by emailing **security@spaceleam.io** (or open a private security advisory on GitHub).

Please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

### Response Timeline

- **Initial response**: Within 48 hours
- **Status update**: Within 7 days
- **Resolution target**: Within 30 days for critical issues

### Disclosure Policy

We follow responsible disclosure. Please do not publicly disclose the vulnerability until we have released a fix and notified users.

## Security Best Practices

When using Cadence in production:

1. **Keep updated** - Always use the latest stable version
2. **Monitor metrics** - Track rate limiter rejections for anomalies
3. **Set appropriate limits** - Configure capacity based on your application requirements
4. **Validate inputs** - Ensure user identifiers for per-user limiting are properly sanitized
