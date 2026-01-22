ANDROID_HOME=$(PWD)/../android
export ANDROID_HOME

BUILD_TOOLS_VERSION != sed -n 's/^ *- build-tools-//p' .travis.yml
ANDROID_PLATFORM_VERSION != (sed -n 's/^ *- yes | sdkmanager "platforms;android-\([0-9]*\)"/\1/p' .travis.yml; echo 26) | head -n1

gradle != which gradle >/dev/null && echo gradle || echo bash gradlew
sdkmanager != which sdkmanager >/dev/null && echo sdkmanager || echo $(ANDROID_HOME)/tools/bin/sdkmanager

SDKS = "platforms;android-$(ANDROID_PLATFORM_VERSION)" "build-tools;$(BUILD_TOOLS_VERSION)" "extras;google;m2repository"
ANDROID_CLI_TOOLS_ZIP=commandlinetools-linux-7302050_latest.zip

DEBUG_APK = app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK = app/build/outputs/apk/release/app-release-unsigned.apk

.DEFAULT_GOAL = app-release-unsigned.apk
.PHONY: assembleRelease clean clean-global-caches install tasks

exec:
	@if [ "$(sh)" ]; then sh -c "$(sh)"; else exec bash; fi

tasks:
	$(gradle) tasks

install: app-release-unsigned.apk
	adb install $^

app-release-unsigned.apk: $(RELEASE_APK)
	ln -sf $^ .

$(RELEASE_APK): assembleRelease

BUILD_DIRS = app/build/ build/ localeapi/build/ wear/build/
clean:
	rm -r .gradle/ $(BUILD_DIRS)

GLOBAL_CACHE_DIRS = ~/.android/build-cache ~/.android/cache/
clean-global-caches:
	rm -r $(GLOBAL_CACHE_DIRS)

../$(ANDROID_CLI_TOOLS_ZIP):
	wget -c https://dl.google.com/android/repository/$(ANDROID_CLI_TOOLS_ZIP) -O $@.partial
	mv $@.partial $@

$(ANDROID_HOME):
	mkdir $@

unzipped-tools-stamp: ../$(ANDROID_CLI_TOOLS_ZIP)
	unzip -d $(ANDROID_HOME) $^
	touch $@

$(sdkmanager): | installed-sdkmanager-stamp

installed-sdkmanager-stamp: ../$(ANDROID_CLI_TOOLS_ZIP) | $(ANDROID_HOME)
	unzip -d $(ANDROID_HOME) $^
	mv -T $(ANDROID_HOME)/cmdline-tools/ $(ANDROID_HOME)/tools/
	touch $@

assembleRelease: | sdk-update-stamp
	$(gradle) assembleRelease

sdk-update: sdk-update-stamp

sdk-update-stamp: | $(sdkmanager)
	yes | $(sdkmanager) $(SDKS)  $(foreach x, $(EXTRA_SDKS),"$x")
	yes | $(sdkmanager) --update
	yes | $(sdkmanager) --licenses
