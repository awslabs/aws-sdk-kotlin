import Builder
import argparse
import os
import sys

class SetUpstreamVersions(Builder.Action):
    """
    Inspects the upstream dependencies for their actual versions and replaces them in `aws-sdk-kotlin/gradle.properties`.

    This ensures that when `aws-sdk-kotlin` is built with a dependency from a branch that has declared a different version
    than what is set in `gradle.properties` that it works correctly.
    """

    def run(self, env):
        if env.project.name != "aws-sdk-kotlin":
            # not the root project, probably building as a consumer
            proj = Builder.Project.find_project("aws-sdk-kotlin")
        else:
            proj = env.project

        deps = proj.config.get("upstream")

        discovered_versions = {}
        for d in deps:
            dep_version = _get_dependency_version(env, d)
            if dep_version is not None:
                discovered_versions[d.name] = dep_version

        print("discovered dependency versions: {}".format(discovered_versions))
        if "smithy-kotlin" in discovered_versions:
            _replace_version_catalog_version(proj, "smithy-kotlin-runtime-version", discovered_versions["smithy-kotlin"])

        if "aws-crt-kotlin" in discovered_versions:
            _replace_gradle_property(proj, "crt-kotlin-version", discovered_versions["aws-crt-kotlin"])


def _replace_gradle_property(proj, prop_name, new_value):
    """
    Replaces the named property with the value if property name exists in gradle.properties
    """
    gradle_props = os.path.join(proj.path, "gradle.properties")

    with open(gradle_props, "r") as f:
        lines = f.readlines()

    with open(gradle_props, "w") as f:
        for line in lines:
            needle = "{}=".format(prop_name)
            if needle in line:
                replacement = "{}={}\n".format(prop_name, new_value)
                f.write(replacement)
                print("replaced {} with {}".format(line.strip(), replacement.strip()))
            else:
                f.write(line)

def _replace_version_catalog_version(proj, prop_name, new_value):
    """
    Replaces the named version catalog version with the value if the version exists in `libs.versions.toml`
    """
    version_catalog = os.path.join(proj.path, "gradle", "libs.versions.toml")

    with open(version_catalog, "r") as f:
        lines = f.readlines()

    with open(version_catalog, "w") as f:
        for line in lines:
            needle = "{} =".format(prop_name)
            if needle in line:
                replacement = "{} = \"{}\"\n".format(prop_name, new_value)
                f.write(replacement)
                print("replaced {} with {}".format(line.strip(), replacement.strip()))
            else:
                f.write(line)

def _get_dependency_version(env, dep): 
    """
    Gets the version of the dependency actually used. This may be different than what
    we have set in our gradle.properties. We need to use the version of the dependency
    actually downloaded
    """
    dep_root = os.path.join(env.deps_dir, dep.name)
    dep_gradle_props = os.path.join(dep_root, "gradle.properties")
    if not os.path.exists(dep_gradle_props):
        dep_proj = Builder.Project.find_project(dep.name)
        dep_gradle_props = os.path.join(dep_proj.path, "gradle.properties")
        if not os.path.exists(dep_gradle_props):
            return None

    with open(dep_gradle_props, "r") as f:
        lines = f.readlines()
    version = next(filter(lambda x: "sdkVersion=" in x, lines), None)

    if version is None:
        return None

    return version.split("=")[1].strip()
