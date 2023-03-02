package cachetest

import org.springframework.cache.CacheManager

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable

// with this annotation dependencies the cache works
// but the dependency injection of the grailsCacheManager did not work
//import grails.plugin.cache.CacheEvict
//import grails.plugin.cache.Cacheable

// with this annotations dependencies the cache didn't work
// but the dependency injection of the grailsCacheManager work
//import org.springframework.cache.annotation.CacheEvict
//import org.springframework.cache.annotation.Cacheable

class HelloController {
	CacheManager grailsCacheManager

	def startTime
	def fromCache

	def index() {
		println "indexController $params"
		fromCache = true
		if (!startTime) {
			startTime = System.currentTimeMillis()
		}
		def text = """
			<p><i>(Request ${System.currentTimeMillis() - startTime}ms after first request.)</i></p>
			<p>The same p in the url shows no queryValue log, another value shows it! Try:</p>
			<p><a href='index?p=6'>same p: localhost:8080/index?p=6</a></p>
			<p><a href='index?p=9'>another p: localhost:8080/index?p=9</a></p>
			<p>Or: when you evict the cache value(s) you see new computing of values:</p>
			<p><a href='index?p=6&e'>evict p=6 from cache: localhost:8080/index?p=6&e</a></p>
			<p><a href='index?p=9&e'>evict p=9 from cache: localhost:8080/index?p=9&e</a></p>
			<p><a href='index?p=9&e=all'>evict all valurs: localhost:8080/index?p=9&e=all</a></p>
			<p>Or restart example:</p>
			<p><a href='index'>restart: localhost:8080/index</a></p>
		"""
		if (!params.p) {
			evictAllValues()
			render "<p>View the server logs and enter a url with an int as parameter p:</p><p><a href='index?p=6'>domain/index?p=6</a></p>"
			return
		}
		if (params.e != null) {
			if (params.e == 'all') {
				evictAllValues()
				render "<br/>$text Cache evicted all in named cache 'test'"
			} else {
				evictValue(params.p)
				render "<br/>$text Cache evicted for ${params.p}"
			}
			return
		}
		def value = getValue(params.p)
		def fromCacheText = ''
		if (fromCache) {
			fromCacheText = '<b>(from Cache)</b>'
		} else {
			fromCacheText = '(fresh Value)'
		}
		def cacheString = cacheEntriesToString()
		render "Hello Cache Test! $value $fromCacheText $text <br><pre>$cacheString</pre>"
	}

	private cacheEntriesToString() {
		def cacheString = ''
		cacheString = cacheString << "grailsCacheManager: $grailsCacheManager\n"
		def cacheNames = grailsCacheManager.getCacheNames()
		cacheString = cacheString << "cacheNames: $cacheNames\n"
		cacheNames.each {
			def cache = grailsCacheManager.getCache(it)
			def allKeys = cache.getAllKeys()
			cacheString = cacheString << "  named cache '$it' (${allKeys.size()}) entries:\n"
			allKeys.each{ key ->
				cacheString = cacheString << "    entry: ${key.simpleKey} = ${cache.get(key).get()}\n"
			}
		}
		return cacheString
	}

	@Cacheable('test')
	def getValue(param) {
		println "===== compute value for $param"
		fromCache = false
		def result = (param as int) * 101
		println "===== result: $result"
		return result
	}

	@CacheEvict('test')
	def evictValue(param) {
		println "============== CacheEvict for $param in named cache 'test'"
	}

	@CacheEvict(value = "test", allEntries = true)
	def evictAllValues() {
		println "============== CacheEvict for all values in named cache 'test'"
	}
}
