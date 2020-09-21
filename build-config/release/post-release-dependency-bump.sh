# ---------------------------------------------------------------------------
#  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

IFS="="
while read -r property value
do
    if [[ $property = "ballerinaLangVersion" ]] && [[ $value = *Preview* ]]
      then
        oldVersion=$(echo $value | cut -d'w' -f 2)
        newVersion=`expr $oldVersion + 1`
        if [ ! -z "$newVersion" ]
          then
            version=$(echo $(echo $value | cut -d'w' -f 1)w$newVersion-SNAPSHOT)
            sed -i "s/ballerinaLangVersion=\(.*\)/ballerinaLangVersion=$version/g" gradle.properties
        fi
    fi
    if [[ $property = stdlib* ]]
      then
        oldVersion=$(echo $value | cut -d'.' -f 3)
        newVersion=`expr $oldVersion + 1`
        if [ ! -z "$newVersion" ]
          then
            version=$(echo $(echo $value | cut -d'.' -f 1).$(echo $value | cut -d'.' -f 2).$newVersion-SNAPSHOT)
            sed -i "s/$property=\(.*\)/$property=$version/g" gradle.properties
          fi
    fi
done < gradle.properties
