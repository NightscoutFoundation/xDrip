# How to translate/localize xDrip

TODO there should be easy documentation for non-programmers how to do translation
TODO should arrays.xml be translate too ? there is only german translation
TODO how to add new language ?

## How to keep translation resources up to date with english changes

This snippet will show strings and arrays that have changed in english from last commit of translation (german `de` in this snippet)

```bash
#!/usr/bin/env bash
LNG=de

EN=app/src/main/res/values/arrays.xml
FN=app/src/main/res/values-${LNG}/arrays-${LNG}.xml
c=$(git log -n 1 --pretty=format:%H -- $FN)
git diff ${c}.. $EN

EN=app/src/main/res/values/strings.xml
FN=app/src/main/res/values-${LNG}/strings-${LNG}.xml
c=$(git log -n 1 --pretty=format:%H -- $FN)
git diff $c.. $EN
```
