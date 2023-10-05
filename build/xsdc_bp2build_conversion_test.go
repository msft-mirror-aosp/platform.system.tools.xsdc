// Copyright 2023 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package xsdc

import (
	"testing"

	"android/soong/android"
	"android/soong/bp2build"
	"android/soong/cc"
	"android/soong/java"
)

const (
	cc_preamble = `
	cc_library {
		name: "libxml2",
	}
	cc_library {
		name: "libtinyxml2",
	}
	cc_library {
		name: "libxsdc-utils",
	}
	`
	java_preamble = `
	java_library {
		name: "stub-annotations",
		sdk_version: "current",
	}
	java_library {
		name: "kxml2-2.3.0",
		sdk_version: "current",
		host_supported: true,
	}
	`
)

func runXsdConfigTest(t *testing.T, tc bp2build.Bp2buildTestCase) {
	t.Parallel()
	tc.StubbedBuildDefinitions = append(tc.StubbedBuildDefinitions,
		"libxml2", "libtinyxml2", "libxsdc-utils", "stub-annotations", "kxml2-2.3.0")
	bp2build.RunBp2BuildTestCase(
		t,
		func(ctx android.RegistrationContext) {
			cc.RegisterLibraryBuildComponents(ctx)
			ctx.RegisterModuleType("java_library", java.LibraryFactory)
		},
		tc,
	)
}

func TestXsdConfigSimple(t *testing.T) {
	runXsdConfigTest(t, bp2build.Bp2buildTestCase{
		Description:                "xsd_config simple",
		ModuleTypeUnderTest:        "xsd_config",
		ModuleTypeUnderTestFactory: xsdConfigFactory,
		Blueprint: cc_preamble + java_preamble + `xsd_config {
	name: "foo",
	srcs: ["foo.xsd"],
}`,
		ExpectedBazelTargets: []string{
			bp2build.MakeBazelTargetNoRestrictions("filegroup", "foo", bp2build.AttrNameToString{
				"srcs": `["foo.xsd"]`,
			}),
			bp2build.MakeBazelTargetNoRestrictions("cc_xsd_config_library", "foo-cpp", bp2build.AttrNameToString{
				"src":                         `"foo.xsd"`,
				"deps":                        `[":libxsdc-utils"]`,
				"implementation_dynamic_deps": `[":libxml2"]`,
			}),
			bp2build.MakeBazelTargetNoRestrictions("java_xsd_config_library", "foo-java", bp2build.AttrNameToString{
				"src": `"foo.xsd"`,
				"deps": `[":stub-annotations"] + select({
        "//build/bazel/platforms/os:android": [],
        "//conditions:default": [":kxml2-2.3.0"],
    })`,
				"sdk_version": `"core_current"`,
			}),
		},
	})
}

func TestXsdConfig(t *testing.T) {
	runXsdConfigTest(t, bp2build.Bp2buildTestCase{
		Description:                "xsd_config",
		ModuleTypeUnderTest:        "xsd_config",
		ModuleTypeUnderTestFactory: xsdConfigFactory,
		Blueprint: cc_preamble + java_preamble + `xsd_config {
	name: "foo",
	srcs: ["foo.xsd"],
	include_files: ["foo.include.xsd"],
	package_name: "foo",
	gen_writer: true,
	enums_only: true,
	boolean_getter: true,
	tinyxml: true,
	root_elements: ["root_element"],
}`,
		ExpectedBazelTargets: []string{
			bp2build.MakeBazelTargetNoRestrictions("filegroup", "foo", bp2build.AttrNameToString{
				"srcs": `[
        "foo.xsd",
        "foo.include.xsd",
    ]`,
			}),
			bp2build.MakeBazelTargetNoRestrictions("cc_xsd_config_library", "foo-cpp", bp2build.AttrNameToString{
				"src":                         `"foo.xsd"`,
				"include_files":               `["foo.include.xsd"]`,
				"package_name":                `"foo"`,
				"gen_writer":                  `True`,
				"enums_only":                  `True`,
				"boolean_getter":              `True`,
				"tinyxml":                     `True`,
				"root_elements":               `["root_element"]`,
				"deps":                        `[":libxsdc-utils"]`,
				"implementation_dynamic_deps": `[":libtinyxml2"]`,
			}),
			bp2build.MakeBazelTargetNoRestrictions("java_xsd_config_library", "foo-java", bp2build.AttrNameToString{
				"src":            `"foo.xsd"`,
				"include_files":  `["foo.include.xsd"]`,
				"package_name":   `"foo"`,
				"gen_writer":     `True`,
				"boolean_getter": `True`,
				"root_elements":  `["root_element"]`,
				"deps": `[":stub-annotations"] + select({
        "//build/bazel/platforms/os:android": [],
        "//conditions:default": [":kxml2-2.3.0"],
    })`,
				"sdk_version": `"core_current"`,
			}),
		},
	})
}

func TestCcAndJavaLibrariesUseXsdConfigGenSrcs(t *testing.T) {
	runXsdConfigTest(t, bp2build.Bp2buildTestCase{
		Description:                "cc_library and java_library use srcs generated from xsd_config",
		ModuleTypeUnderTest:        "xsd_config",
		ModuleTypeUnderTestFactory: xsdConfigFactory,
		StubbedBuildDefinitions:    []string{"foo"},
		Blueprint: cc_preamble + java_preamble + `
xsd_config {
	name: "foo",
	srcs: ["foo.xsd"],
}
cc_library {
	name: "cclib",
	generated_sources: ["foo"],
	generated_headers: ["foo"],
}
java_library {
	name: "javalib",
	srcs: [
		"A.java",
		":foo"
	],
		sdk_version: "current",
}`,
		ExpectedBazelTargets: []string{
			bp2build.MakeBazelTarget("cc_library_static", "cclib_bp2build_cc_library_static", bp2build.AttrNameToString{
				"local_includes":                    `["."]`,
				"implementation_whole_archive_deps": `[":foo-cpp"]`,
			}),
			bp2build.MakeBazelTarget("cc_library_shared", "cclib", bp2build.AttrNameToString{
				"local_includes":                    `["."]`,
				"implementation_whole_archive_deps": `[":foo-cpp"]`,
			}),
			bp2build.MakeBazelTarget("java_library", "javalib", bp2build.AttrNameToString{
				"srcs":        `["A.java"]`,
				"deps":        `[":foo-java"]`,
				"exports":     `[":foo-java"]`,
				"sdk_version": `"current"`,
			}),
			bp2build.MakeNeverlinkDuplicateTarget("java_library", "javalib"),
		},
	})
}

func TestCcAndJavaLibrariesUseXsdConfigGenSrcsNoHdrs(t *testing.T) {
	runXsdConfigTest(t, bp2build.Bp2buildTestCase{
		Description:                "cc_library and java_library use srcs generated from xsd_config",
		ModuleTypeUnderTest:        "xsd_config",
		ModuleTypeUnderTestFactory: xsdConfigFactory,
		StubbedBuildDefinitions:    []string{"foo"},
		Blueprint: cc_preamble + java_preamble + `
xsd_config {
	name: "foo",
	srcs: ["foo.xsd"],
}
cc_library {
	name: "cclib",
	generated_sources: ["foo"],
}`,
		ExpectedBazelTargets: []string{
			bp2build.MakeBazelTarget("cc_library_static", "cclib_bp2build_cc_library_static", bp2build.AttrNameToString{
				"local_includes":                    `["."]`,
				"implementation_whole_archive_deps": `[":foo-cpp"]`,
			}),
			bp2build.MakeBazelTarget("cc_library_shared", "cclib", bp2build.AttrNameToString{
				"local_includes":                    `["."]`,
				"implementation_whole_archive_deps": `[":foo-cpp"]`,
			}),
		},
	})
}

func TestCcAndJavaLibrariesUseXsdConfigGenSrcsExportHeaders(t *testing.T) {
	runXsdConfigTest(t, bp2build.Bp2buildTestCase{
		Description:                "cc_library export headers from xsd_config",
		ModuleTypeUnderTest:        "xsd_config",
		ModuleTypeUnderTestFactory: xsdConfigFactory,
		StubbedBuildDefinitions:    []string{"foo"},
		Blueprint: cc_preamble + java_preamble + `
xsd_config {
	name: "foo",
	srcs: ["foo.xsd"],
}
cc_library {
	name: "cclib",
	generated_sources: ["foo"],
	generated_headers: ["foo"],
	export_generated_headers: ["foo"],
}`,
		ExpectedBazelTargets: []string{
			bp2build.MakeBazelTarget("cc_library_static", "cclib_bp2build_cc_library_static", bp2build.AttrNameToString{
				"local_includes":     `["."]`,
				"whole_archive_deps": `[":foo-cpp"]`,
			}),
			bp2build.MakeBazelTarget("cc_library_shared", "cclib", bp2build.AttrNameToString{
				"local_includes":     `["."]`,
				"whole_archive_deps": `[":foo-cpp"]`,
			}),
		},
	})
}
