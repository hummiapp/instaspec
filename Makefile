.PHONY: all setup update test lint

all: setup test

setup:
	clojure -P

update:
	clojure -M:outdated --every --write

test:
	clojure -M:test

lint:
	clojure -M:lint
