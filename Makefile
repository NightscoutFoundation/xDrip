ANDROID_HOME=$(PWD)/../android
export ANDROID_HOME

BUILD_TOOLS_VERSION := $(shell sed -n 's/^ *- build-tools-//p' .travis.yml)
ANDROID_PLATFORM_VERSION := $(shell sed -n 's/^ *- yes | sdkmanager "platforms;android-\([0-9]*\)"/\1/p' .travis.yml || echo 26)


GIT_DISCOVERY_ACROSS_FILESYSTEM=y
export GIT_DISCOVERY_ACROSS_FILESYSTEM

SDKS = "platforms;android-$(ANDROID_PLATFORM_VERSION)" "build-tools;$(BUILD_TOOLS_VERSION)" "extras;google;m2repository"
ANDROID_SDK_ZIP=sdk-tools-linux-4333796.zip

DEBUG_APK = app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK = app/build/outputs/apk/release/app-release-unsigned.apk

.DEFAULT_GOAL = app-release-unsigned.apk
.PHONY: assembleRelease clean clean-global-caches install tasks

gradle := $(shell [ "$(which gradle >/dev/null)" ] && echo gradle || echo bash gradlew)

exec:
	@if [ "$(sh)" ]; then sh -c "$(sh)"; else exec bash; fi

tasks:
	$(gradle) tasks

install: app-release-unsigned.apk
	adb install $^

app-release-unsigned.apk: $(RELEASE_APK)
	ln -sf $^ .

$(RELEASE_APK): assembleRelease

assembleRelease: | installed-android-sdk-stamp
	$(gradle) assembleRelease

BUILD_DIRS = app/build/ build/ localeapi/build/ wear/build/
clean:
	rm -r .gradle/ $(BUILD_DIRS)

GLOBAL_CACHE_DIRS = ~/.android/build-cache ~/.android/cache/
clean-global-caches:
	rm -r $(GLOBAL_CACHE_DIRS)

../$(ANDROID_SDK_ZIP):
	wget -c https://dl.google.com/android/repository/$(ANDROID_SDK_ZIP) -O $@.partial
	mv $@.partial $@

$(ANDROID_HOME):
	mkdir $@

unzipped-android-sdk-stamp: ../$(ANDROID_SDK_ZIP)
	unzip -d $(ANDROID_HOME) $^
	touch $@

sdkmanager = $(ANDROID_HOME)/tools/bin/sdkmanager

installed-android-sdk-stamp: | unzipped-android-sdk-stamp $(ANDROID_HOME)
	yes | $(sdkmanager) --licenses
	yes | $(sdkmanager) $(SDKS)
	touch $@

sdk-update:
	yes | $(sdkmanager) $(SDKS)  $(foreach x, $(EXTRA_SDKS),"$x")
	yes | $(sdkmanager) --update
	yes | $(sdkmanager) --licenses
