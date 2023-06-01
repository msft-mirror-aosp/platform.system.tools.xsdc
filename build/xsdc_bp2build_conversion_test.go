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
		bazel_module: {bp2build_available: false},
	}
	cc_library {
		name: "libtinyxml2",
		bazel_module: {bp2build_available: false},
	}
	cc_library {
		name: "libxsdc-utils",
		bazel_module: {bp2build_available: false},
	}
	`
	java_preamble = `
	java_library {
		name: "stub-annotations",
		bazel_module: {bp2build_available: false},
	}
	java_library {
		name: "kxml2-2.3.0",
		host_supported: true,
		bazel_module: {bp2build_available: false},
	}
	`
)

func runXsdConfigTest(t *testing.T, tc bp2build.Bp2buildTestCase) {
	t.Parallel()
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
