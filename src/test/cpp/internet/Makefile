ALL: test_internet

CXXFLAGS+=-std=c++17 -Wall -Wextra -pedantic

test_internet.o: test_internet.cpp internet.h
	$(CXX) $(CXXFLAGS) -o $@ -c $<

test_internet: test_internet.o
	$(CXX) -o $@ $^

.PHOHY: ALL
