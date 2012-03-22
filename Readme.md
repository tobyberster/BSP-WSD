Binary Spatter Code - Word Sense Disambiguation (BSP-WSD)
=========

Java tool that utilizes the Semantic Vectors Package to disambiguate word senses from the NLM Disambiguation data set.

What does it do?
----------------

Performs word sense disambiguation on a set of 50 words stored in a lucene index. 

You may change the vector size and the context around the ambiguous term in the code to change the behavior of the disambiguation algorithm. 

Warning: This code is not yet optimized and was created only for research purposes. If you do find anything wrong with it, please let me know so I can update it accordingly.

Requirements
------------

- Semantic Vectors Package: 
  For more information about this package, go to http://code.google.com/p/semanticvectors/

- National Library of Medicine - Word Sense Disambiguation data set:
  For more information about this data set, go to http://wsd.nlm.nih.gov/
  Included in this release is a lucene index of this data set. 
  
- Stopword List by Chris Buckley and Gerard Salton at Cornell University:
  Fore more information about this list, go to http://www.lextek.com/manuals/onix/stopwords2.html

- Bobcat MW Utils by Manuel Wahle:
  For more information about this package, go to https://github.com/mwahle/Hello-Wiki/lib

Data
----

- NLM - WSD data set:
  Included as lucene index in tarball format in the data folder. Untar and use.
- Stopword List:
  Included as *.txt file in the data folder.

Usage
-----

  java BuildDisambiguationIndex PATH_TO_DISAMBIGUATION_INDEX
  
Dont forget to set the memory limit quite high, or else it will crash right away.
For my purposes I usually set it to the following:
  -Xmx16G


Tests
-----

No unit tests have been created for this project. Three separate researchers looked over the code and found no logical issues within the code. Please feel free to make changes and share them with the community.

Author
-------

Toby Berster ([@wurzelgogerer](http://twitter.com/wurzelgogerer))
toby.berster@gmail.com

License
-------

Copyright (c) 2012 Toby Berster

Licensed under the New BSD License.