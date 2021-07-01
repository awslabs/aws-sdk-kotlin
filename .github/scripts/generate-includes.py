#!/usr/bin/env python3
"""
Converts a list of model file paths to a formatted includes list for passing to bootstrap
"""
import sys
import os

not_supported_yet = ["ec2", "location", "elasticbeanstalk", "marketplacecommerceanalytics"]


def main():
    lines = sys.stdin.readlines()
    services = [os.path.basename(x).split(".")[0] for x in lines]
    services = ["+" + x for x in services if x not in not_supported_yet]
    sys.stdout.write(",".join(services))


if __name__ == "__main__":
    main()


