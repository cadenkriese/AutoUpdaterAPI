<p align="center">
<a href="https://ci.flogi.cc/job/AutoUpdaterAPI"><img src="https://i.imgur.com/pA4xNdY.png"></a>
<br>
<a href="https://ci.flogi.cc/job/AutoUpdaterAPI"><img src="https://img.shields.io/jenkins/s/http/ci.flogi.cc/job/AutoUpdaterAPI.svg?style=for-the-badge"></a>
<a href="https://github.com/fl0gic/AutoUpdaterAPI/issues"><img src="https://img.shields.io/github/issues/fl0gic/AutoUpdaterAPI.svg?logo=github&style=for-the-badge"></a>
<a href="https://github.com/fl0gic/AutoUpdaterAPI/commits/master"><img src="https://img.shields.io/github/last-commit/fl0gic/AutoUpdaterAPI.svg?logo=github&style=for-the-badge"></a>
<a href="https://github.com/fl0gic/AutoUpdaterAPI/blob/master/LICENSE"><img src="https://img.shields.io/github/license/fl0gic/autoupdaterapi.svg?style=for-the-badge"></a>
</p>

## Project
AutoUpdater is an API for seamlessly updating your free and premium Spigot plugins without requiring a restart or reload.

### Key Features

💡 **SIMPLE** - AutoUpdaterAPI is simple to use and looks great with your plugin.

🥊 **LIGHTWEIGHT** - AutoUpdater fits into any plugin, taking up less than 100kb of space when shaded in.

🏎 **FAST** - With the right configuration, AutoUpdater can update your plugin in less than half a second.

🚀 **POWERFUL** - AutoUpdater can automatically download and install premium and free resources and utilizes Bukkit's multithreading API to make downloads as fast and non-intensive as possible.

🔒 **SECURE** - Since AutoUpdater needs to hold your users' spigot credentials (for premium resource updating) it uses 2048 bit industry standard AES encryption with randomly generated keys for each system.

🏳 **CLEAN** - AutoUpdater has a clean and customizable UI. It has a custom format for logging errors, and you can even turn on a full debug mode through the main class.

💭 **OPEN** - AutoUpdater is fully open source and welcomes contributions.

## Usage
### Example Usage
```java
    import cc.flogi.dev.autoupdater.internal.AutoUpdaterAPI;

    private AutoUpdaterAPI autoUpdaterAPI;

    @Override
    public void onEnable() {
        //Initialize AutoUpdaterAPI on plugin enable, specifying if premium support should be enabled or not.
        autoUpdaterAPI = new AutoUpdaterAPI(this, true);
    }

    public void update(Player initiator) {
        boolean initialize = true;
        boolean replaceOldVersion = true;
        int spigotId = 24610;
        UpdateLocale locale = autoUpdaterAPI.createLocaleBuilder()
                        .updatingMsgNoVar("Place")
                        .updatingMsg("Your")
                        .downloadingMsg("Own")
                        .completionMsg("Messages")
                        .failureMsg("Here")
                        .build();

        autoUpdaterAPI.createSpigotPluginUpdater(this, initiator, spigotId, locale, initialize, replaceOldVersion, 
        (pluginName, success, exception) -> {
            if(success)
                Bukkit.broadcastMessage("Successfully updated " + pluginName + "!");
        }).update();
    }
```

### Update Cycle
When you call the #update method, this is what AutoUpdater does.
1. AutoUpdater downloads the specified plugin.
1. If the plugin is to be installed now,
    1. AutoUpdater loads itself as a separate plugin utility.
    1. The plugin utility unloads the old version and loads the new version.
    1. The plugin utility removes itself completely.
    1. The completion runnable is called, and the initiator player is notified.
1. If the plugin is to be installed later,
    1. The plugin is placed in an internal cache folder with a meta file.
    1. The completion runnable is called, and the initiator player is notified.
    1. Whenever AutoUpdaterAPI#installCachedUpdates() is called (preferably in #onDisable), the plugin is moved to the plugins folder, and the old version is removed if specified.
    


## Maven

### Repository
```xml
<repository>
    <id>flogic-releases</id>
    <url>https://repo.flogi.cc/artifactory/flogic-releases</url>
</repository>
```
### Dependency
```xml
<dependency>
    <groupId>cc.flogi.dev</groupId>
    <artifactId>autoupdater-core</artifactId>
    <version>3.0.1</version>
</dependency>
```
