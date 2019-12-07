/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package prefab.release

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*

data class Version(val number: String, val qualifier: String?) {
    override fun toString(): String = "$number${qualifier ?: ""}"

    companion object {
        val pattern = Regex(
            """^(\d+\.\d+\.\d+)(-(?:(?:alpha|beta|milestone|rc)\d+))?$"""
        )

        fun parse(versionString: String): Version {
            val match = pattern.find(versionString)
            require(match != null) {
                "Version string $versionString did not match expected pattern"
            }
            return Version(match.groupValues[1],
                match.groupValues[2].takeIf { it.isNotEmpty() })
        }
    }
}

fun Process.waitForSuccess(errorMessage: (Process) -> String) {
    waitFor().let {
        if (it != 0) {
            throw RuntimeException(errorMessage(this))
        }
    }
}

fun Process.waitForSuccess() {
    waitForSuccess() { "Process failed" }
}

class ReleaseTool(
    private val version: Version, private val fromCommit: String
) {
    private val remoteUrl = "git@github.com:google/prefab.git"

    private val branchName = "release-v$version"
    private val tagName = "v$version"
    private val commitMessage = "Release Prefab v$version."
    private val tagMessage = "Prefab release version $version."

    private fun verifyNoLocalModifications() {
        val repoHasModifications = ProcessBuilder(
            listOf(
                "git", "diff-index", "--quiet", "HEAD"
            )
        ).start().waitFor() != 0
        if (repoHasModifications) {
            System.err.println("Repository has uncommitted modifications.")
            System.exit(1)
        }
    }

    private fun findUpstreamRemoteName(): String {
        val proc = ProcessBuilder(listOf("git", "remote", "-v")).start()
        proc.waitForSuccess() { "Unable to get list of git remotes" }
        val output: String =
            proc.inputStream.bufferedReader().use(BufferedReader::readText)
        val remotePattern = """^(\S+)\s+(\S+)\s+\((?:fetch|push)\)$""".toRegex()
        for (line in output.lines()) {
            val match = remotePattern.find(line) ?: throw RuntimeException(
                "git remote -v output does not match expected pattern: $line"
            )

            val (name, url) = match.destructured
            if (url == remoteUrl) {
                return name
            }
        }

        throw RuntimeException("Could not find remote matching $remoteUrl")
    }

    private fun verifyCommitIsSubmitted(remoteName: String) {
        val proc = ProcessBuilder(
            listOf(
                "git", "branch", "-r", "--contains", fromCommit
            )
        ).start()
        proc.waitForSuccess() {
            "Unable to detemine which branches contain $fromCommit"
        }
        val output: String =
            proc.inputStream.bufferedReader().use(BufferedReader::readText)
        val remoteBranchName = "$remoteName/master"
        for (line in output.lines()) {
            if (line.trim() == remoteBranchName) {
                return
            }
        }

        throw RuntimeException("Did not find fromCommit in $remoteBranchName")
    }

    private fun verifyGitState(remoteName: String) {
        verifyNoLocalModifications()
        verifyCommitIsSubmitted(remoteName)
    }

    private fun createBranch() {
        println("Creating branch for release")
        ProcessBuilder(
            listOf(
                "git", "checkout", "-b", branchName, fromCommit
            )
        ).inheritIO().start()
            .waitForSuccess() { "Failed to create branch $branchName" }
    }

    private fun updateProps() {
        println("Updating gradle.properties")
        val propsFile = File("gradle.properties")
        Properties().apply {
            load(FileReader(propsFile))
            setProperty("prefab.version", version.number)
            setProperty("prefab.qualifier", version.qualifier ?: "")
            store(FileWriter(propsFile), null)
        }
    }

    private fun testChanges() {
        println("Building and running tests")
        ProcessBuilder(
            listOf(
                File("gradlew").absolutePath,
                "-Pprefab.release",
                "clean",
                "release"
            )
        ).inheritIO().start().waitForSuccess()
    }

    private fun commitProps() {
        println("Committing changes")
        ProcessBuilder(
            listOf(
                "git", "commit", "-a", "-m", commitMessage
            )
        ).inheritIO().start().waitForSuccess() { "Failed to commit changes" }
    }

    private fun tagRelease() {
        println("Tagging release")
        ProcessBuilder(
            listOf(
                "git", "tag", "-a", "$tagName", "-m", tagMessage
            )
        ).inheritIO().start().waitForSuccess() { "Failed to create tag" }
    }

    fun run() {
        val remote = findUpstreamRemoteName()
        verifyGitState(remote)
        println("Creating release $version")

        createBranch()
        updateProps()
        testChanges()
        commitProps()
        tagRelease()

        val repository = File("build/repository").absolutePath
        println("Finished preparing release.")
        println("The following packages have been installed to $repository:")
        println("\tcom.google.prefab:api:$version")
        println("\tcom.google.prefab:cli:$version")
        println("To finalize, run `git push --tags $remote $tagName`")
    }
}

require(args.size in 1..2) { "usage: release RELEASE_NAME [FROM_COMMIT]" }
ReleaseTool(Version.parse(args[0]), args.getOrElse(1) { "HEAD" }).run()