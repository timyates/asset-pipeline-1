/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package asset.pipeline

class CacheManager {
	static cache = [:]

	static def findCache(fileName, md5, originalFileName = null) {
		def cacheRecord = CacheManager.cache[fileName]

		if(cacheRecord && cacheRecord.md5 == md5 && cacheRecord.originalFileName == originalFileName) {
			def cacheFiles = cacheRecord.dependencies.keySet()
			def expiredCacheFound = cacheFiles.find { cacheFileName ->
				def cacheFile = AssetHelper.fileForUri(cacheFileName)
				if(!cacheFile)
					return true
				def depMd5 = AssetHelper.getByteDigest(cacheFile.inputStream.bytes)
				if(cacheRecord.dependencies[cacheFileName] != depMd5) {
					return true
				}
				return false
			}

			if(expiredCacheFound) {
				CacheManager.cache.remove(fileName)
				return null
			}
			return cacheRecord.processedFileText
		} else if (cacheRecord) {
			CacheManager.cache.remove(fileName)
			return null
		}
	}

	static def createCache(fileName, md5Hash, processedFileText, originalFileName = null) {
		def cacheRecord = CacheManager.cache[fileName]
		if(cacheRecord) {
			CacheManager.cache[fileName] = cacheRecord + [
				md5: md5Hash,
				originalFileName: originalFileName,
				processedFileText: processedFileText
			]
		} else {
			CacheManager.cache[fileName] = [
				md5: md5Hash,
				originalFileName: originalFileName,
				processedFileText: processedFileText,
				dependencies: [:]
			]
		}

	}

	static def addCacheDependency(fileName, dependentFile) {
		def cacheRecord = CacheManager.cache[fileName]
		if(!cacheRecord) {
			CacheManager.createCache(fileName, null, null)
			cacheRecord = CacheManager.cache[fileName]
		}
		def newMd5 = AssetHelper.getByteDigest(dependentFile.inputStream.bytes)
		cacheRecord.dependencies[dependentFile.path] = newMd5
	}
}
