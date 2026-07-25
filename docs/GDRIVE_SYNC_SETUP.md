# Google Drive sync — one-time setup

Yomu's Drive backup/restore (More → **Google Drive sync**) needs a Google Cloud OAuth
client tied to this app's package + signing key. This is a one-time setup you do in the
Google Cloud Console; without it, "Sign in with Google" will fail when it requests the
Drive scope.

## Steps

1. **Create/choose a project** at <https://console.cloud.google.com>.
2. **Enable the Drive API**: APIs & Services → Library → *Google Drive API* → Enable.
3. **OAuth consent screen**: configure it (External is fine), and under *Test users*
   add the Google account you'll sign in with. (Unpublished apps only allow test users.)
   The only scope needed is `.../auth/drive.appdata` (app-private folder).
4. **Create an OAuth client ID**: APIs & Services → Credentials → Create credentials →
   OAuth client ID → **Android**.
   - Package name: `com.yomu.reader`
   - SHA-1 certificate fingerprint:
     - **debug** (current builds): `75:AE:99:8D:0A:26:19:8E:A4:B5:F7:2B:BC:6D:C1:2B:FD:2B:EF:92`
     - **release**: add your release keystore's SHA-1 here once you create one.

That's it — Android OAuth clients are matched by *package name + SHA-1*, so no
`google-services.json` or client secret is bundled in the app.

## Notes

- Backup writes a single `yomu-sync.json` to Drive's hidden **appDataFolder** (private to
  Yomu; it never touches the user's visible Drive files).
- What's synced: favourited manga (source + identity + metadata), categories and their
  membership, and the list of extension repo URLs. Reading progress and downloaded files
  are **not** synced.
- Restore is additive/last-write-wins per manga: it re-favourites and re-categorises the
  backed-up manga and re-adds the extension repos. It does not delete local manga.
- Installed extension packages are recorded in the backup for reference, but extensions
  must still be installed from their repos on the new device (APKs aren't synced).
