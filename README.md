# ![MoshidonLogo](mastodon/src/main/res/mipmap-xhdpi/ic_launcher_round.png) Moshidon, the material you mastodon client!

## This is the rewrite branch. Things probably won't as you expect here for a while

> A fast, highly customizable, up-to-date fork of [megalodon](https://github.com/sk22/megalodon) adding important features such as a fully federated timeline, unlisted posting, drafts, scheduled posts, bookmarks, and alt text warnings.


## Download Now

<a href="https://play.google.com/store/apps/details?id=org.joinmastodon.android.moshinda"><img height="35" alt="Get it on Google Play" src="img/google-play-badge.png"></a> <a href="https://f-droid.org/pt_BR/packages/org.joinmastodon.android.moshinda"><img height="35" alt="Get it on F-Droid" src="img/f-droid-badge.png"></a> <a href="https://apt.izzysoft.de/fdroid/index/apk/org.joinmastodon.android.moshinda"><img height="35" alt="Get it on IzzyOnDroid" src="img/izzy-badge.png"></a>

[![GitHub Release Download](https://img.shields.io/badge/dynamic/json?color=282C37&label=Download%20APK&query=%24.tag_name&url=https%3A%2F%2Fapi.github.com%2Frepos%2FLucasGGamerM%2Fmoshidon%2Freleases%2Flatest&style=for-the-badge)](https://github.com/LucasGGamerM/moshidon/releases/latest/download/moshidon.apk) [![Translation status](https://translate.codeberg.org/widgets/moshidon/-/svg-badge.svg)](https://translate.codeberg.org/engage/moshidon/) [![GitHub Nightly Download](https://img.shields.io/badge/dynamic/json?color=282C37&label=Download%20Nightly%20APK&query=%24.tag_name&url=https%3A%2F%2Fapi.github.com%2Frepos%2FLucasGGamerM%2Fmoshidon%2Freleases%2Flatest&style=for-the-badge)](https://github.com/LucasGGamerM/moshidon-nightly/releases/latest/download/moshidon-nightly.apk) [![GitHub Nightly Build Download](https://github.com/LucasGGamerM/moshidon/actions/workflows/nightly-builds.yml/badge.svg)](https://github.com/LucasGGamerM/moshidon/actions/workflows/nightly-builds.yml)

## Donate

<a href="https://github.com/sponsors/LucasGGamerM">Github Sponsors</a> | <a href="https://liberapay.com/LucasGGamerM/donate">Liberapay</a> | Monero Wallet Key: `4886mdarcyB6Yf8Qc6vDJBK1fz6ibHFLZUmHb4GZZz9yLGNhcG3XC64e5UZ8dVQYTLZb82W6P9WhteowW4STJEec97Gf22j`

## Key Features

[ screenshot of full timeline in default colour scheme ]
[ screenshot of full timeline in an alt colour scheme ]
[ screenshot of profile page ]
[ screenshot of compose post window ]

### Flexible Timelines

[ Home dropdown menu ]

Under the Home menu by default you can see your active account's timeline, your server's local timeline, and your server's federated timeline. You can also pin hashtags, lists, other servers, or make a custom view of just your posts, your bookmarks, or your favourites for quick access. Then sort these timelines to prioritize the ones you visit most often.

### Multiple Accounts & Crossposting

Sign in to multiple accounts in the same app and easily switch between them. Press and hold on the boost or fave button to boost or fave a post to a different account than the one you are currently browsing with.

[ boost icon pop up select profile ]

### Drafts & Scheduled Posts

Write posts and save them, or schedule them to post later. Edit and delete your drafts.

### Alt Text Tag & Reminder

An unobtrusive ALT tag appears on images with alt text. Clicking on the icon makes the alt text appear. By default, Moshidon will show a warning to add alt text if your post has any attachments lacking alt text. This is for better accessibility, and it can be disabled in settings. You can also hide from your feed all posts that are lacking in alt text.

[ image with alt text icon higlighted ]
[ alt text expanded ]

### Themes & Customization

Moshidon is designed according to Material Design principles. Follow your device's light or dark mode settings or change colour palette - your system's default, purple, black & white, "pitch black" (battery saving) and more. Customize your experience by moving or renaming the publish button, show or hide sensitive media by default, reduce motion, collapse long posts, add haptic feedback, or making the fave button a heart &hearts; or a star &starf;.

### Not Just For Mastodon

Supports features available on other types of fediverse servers such as admin announcements, showing pronouns in user names, post translation, emoji reactions, local-only posting, and markdown or html in posts.

### Fully Federated Feed & Profiles

See all public posts from servers your server federates with and fetch profiles from a user's local server for accurate up to date information.

## And more...
- quote-posts - links to fediverse posts in other posts will be loaded inline like quote-tweets
- manage pinned posts and bookmarks
- manage lists, filters, and most privacy settings
- display pronouns in timelines, threads, and user listings
- get only specific types of notifications (no more finished polls!), limit who you get notifications from, or group all notifications into one.
- automatically add "re:" to beginning of replies with content warnings
- ask before boosting or deleting posts
- when replying to a boosted post automatically mention the person who boosted it
- overlay audio from posts, allowing your existing media to keep playing
- auto-reveal CWs that are the same as ones you've already opened, or always reveal content warnings and sensitive media
- hide media previews in timelines (save data)
- show post interaction counts in timeline
- allow custom emoji in display names
- enable scrolling text for long display names
- hide interaction buttons
- show post dividers


## Installation & Releases

Moshidon is available on GitHub, Google Play, F-Droid, and the IzzyOnDroid repo. All sources provide the same ` moshidon.apk ` stable release. Older releases are available on the [Releases](https://github.com/LucasGGamerM/moshidon/releases) page.

### How to Install from GitHub
[Download the latest stable release from Github](https://github.com/LucasGGamerM/moshidon/releases/latest/download/moshidon.apk) and open it. You might have to accept installing APK files from your browser. Moshidon will automatically check for new updates available on GitHub and offer to download and install them within the app. You can also manually press “Check for updates” at the bottom of the settings page.

### Nightly Version
All ` moshidon-night.apk ` nightly builds can be downloaded on the [Nightly Releases](https://github.com/LucasGGamerM/moshidon-nightly/releases) page. This is an unstable version with an integrated updater for development and testing purposes. If you find any bugs with it, please file a bug report on our [Issues](https://github.com/LucasGGamerM/moshidon/issues) page.

## Building & Contributing

As this app is using Java 17 features, you need JDK 17 or newer to build it. Other than that, everything is pretty standard. You can either import the project into Android Studio and build it from there, or run the following command in the project directory:

```
./gradlew assembleRelease
```

## License

This project is released under the [GPL-3 License](./LICENSE).

## Contact & Support

**<a rel="me" href="https://floss.social/@moshidon">@moshidon@floss.social</a>**

[Official Matrix Chatroom](https://matrix.to/#/#moshidon:floss.social)

[F.A.Q](FAQ.md)

[Moshidon Roadmap](https://github.com/users/LucasGGamerM/projects/1)

