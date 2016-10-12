Completion suggester for Japanese for Elasticsearch
==================================

This plugin includes:
* "kuromoji_suggest" tokenizer, extension for Lucene's Japanese (kuromoji) Analyzer to enable query completion.
* "japanese_completion" suggester, customized completion suggester for Japanese.

You need to install a version matching your Elasticsearch version:

| elasticsearch |  Kuromoji Analysis Suggest Plugin |
|---------------|-----------------------------|
| es-1.4        |     0.9         |
| es-1.5        |     0.10.0         |
| es-1.6        |     0.11.0         |
| es-2.0.0        |     0.20.0         |
| es-2.0.1        |     0.20.1         |
| es-2.0.2        |     0.20.2         |
| es-2.1.0        |     0.21.0         |
| es-2.1.1        |     0.21.1         |
| es-2.1.2        |     0.21.2         |
| es-2.2.0        |     0.22.0         |
| es-2.2.1        |     0.22.1         |
| es-2.2.2        |     0.22.2         |
| es-5.0.0-rc1    |     5.0.0-rc1      |


## Prerequisites
* (Optional) To deal with full-width and half-width characters, elasticsearch-analysis-icu(https://github.com/elasticsearch/elasticsearch-analysis-icu)

## Installation
In order to install the plugin, run:

```sh
# In elasticsearch home directory
bin/plugin install https://github.com/masaruh/elasticsearch-japanese-suggester/releases/download/0.20.0/elasticsearch-japanese-suggester-0.20.0.zip
```

## Exampl Usage

### Create index (0.21.1 or above)
```sh
curl -XPUT "http://localhost:9200/suggest_sample" -d'
{
  "mappings": {
    "test": {
      "properties": {
        "suggest": {
          "type": "japanese_completion",
          "analyzer": "kuromoji_suggest_index",
          "search_analyzer": "kuromoji_suggest_search",
          "payloads": true
        }
      }
    }
  }
}'
```

### Create index (0.21.0 or below)

Use expand=true for indexing and expand=false for searching.
```sh
curl -XPUT 'http://localhost:9200/suggest_sample/' -d'
{
  "index": {
    "analysis": {
      "analyzer": {
        "kuromoji_suggest_index":{
          "tokenizer":"kuromoji_suggest_index"
        },
        "kuromoji_suggest_search":{
          "tokenizer":"kuromoji_suggest_search"
        }
      },

      "tokenizer":{
        "kuromoji_suggest_index":{
          "type": "kuromoji_suggest",
          "expand":true
        },
        "kuromoji_suggest_search":{
          "type": "kuromoji_suggest",
          "expand":false
        }
      }
    }
  }
}
'
```

Then add completion suggester configuration:
```sh
curl -XPUT 'http://localhost:9200/suggest_sample/test/_mapping' -d'
{
  "properties": {
    "suggest": {
      "type": "japanese_completion",
      "analyzer": "kuromoji_suggest_index",
      "search_analyzer": "kuromoji_suggest_search",
      "payloads": true
    }
  }
}
'
```

### Feed document
```sh
curl -XPUT 'http://localhost:9200/suggest_sample/test/1' -d'
{
  "suggest":"東京駅"
}
```

### Search
```sh
curl -XPOST 'http://localhost:9200/suggest_sample/_suggest' -d'
{
    "suggest" : {
        "text" : "とうk",
        "japanese_completion" : {
            "field" : "suggest"
        }
    }
}
'

{
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "failed" : 0
  },
  "suggest" : [ {
    "text" : "とうk",
    "offset" : 0,
    "length" : 3,
    "options" : [ {
      "text" : "東京駅",
      "score" : 1.0
    } ]
  } ]
}

```



License
-------
See LICENSE.txt