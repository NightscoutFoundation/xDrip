ANDROID_HOME=$(PWD)/../android
export ANDROID_HOME
SDKS = "platforms;android-26" "build-tools;27.0.3" "extras;google;m2repository"
ANDROID_SDK_ZIP=sdk-tools-linux-4333796.zip

DEBUG_APK = app/build/outputs/apk/debug/app-debug.apk
RELEASE_APK = app/build/outputs/apk/release/app-release-unsigned.apk

.DEFAULT_GOAL = app-release-unsigned.apk
.PHONY: assembleRelease clean clean-global-caches install

install: app-release-unsigned.apk
	adb install $^

app-release-unsigned.apk: $(RELEASE_APK)
	ln -sf $^ .

$(RELEASE_APK): assembleRelease

assembleRelease: | installed-android-sdk-stamp
	bash gradlew assembleRelease

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
