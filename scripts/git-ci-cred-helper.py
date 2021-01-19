#!/usr/bin/env python
import argparse
import os


def main():
  parser = argparse.ArgumentParser()
  parser.add_argument('operation', action="store", type=str, help="Git action to be performed", choices=["get", "store", "erase"])
  arguments = parser.parse_args()

  if arguments.operation == "get":
      print("username={0}".format(os.environ.get("CI_USER")))
      print("password={0}".format(os.environ.get("CI_ACCESS_TOKEN")))

if __name__ == "__main__":
  main()
