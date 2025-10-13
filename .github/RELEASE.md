# 🚀 Release Process

This project uses GitHub Actions to automate the release process.

## How to Create a New Release

### 1. Preparation

Make sure all changes have been committed and pushed to the `main` branch:

```bash
git checkout main
git pull origin main
```

### 2. Create and Push the Tag

Create a tag with the format `vX.Y.Z` (where X.Y.Z is the semantic version):

```bash
# Example for version 1.0.1
git tag -a v1.0.1 -m "Release version 1.0.1"
git push origin v1.0.1
```

### 3. Automatic Process

Once the tag is pushed, GitHub Actions will:

1. ✅ Checkout the code
2. ✅ Set up Java 11 and Maven
3. ✅ Automatically update the version in `pom.xml`
4. ✅ Compile the project
5. ✅ Run all tests
6. ✅ Generate changelog automatically from commits
7. ✅ Create the GitHub release with:
   - Release name
   - Generated changelog
   - Compiled JAR as artifact
   - Maven installation instructions
   - Automatically generated release notes from GitHub

### 4. Verification

Go to `https://github.com/[your-username]/[your-repo]/releases` to see the newly published release.

## Semantic Versioning

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (X.0.0): Breaking changes incompatible with previous versions
- **MINOR** (x.Y.0): New backward-compatible features
- **PATCH** (x.y.Z): Backward-compatible bug fixes

## Examples

```bash
# Bug fix
git tag -a v1.0.1 -m "Fix: Resolved issue with transitions"
git push origin v1.0.1

# New feature
git tag -a v1.1.0 -m "Feature: Added support for async events"
git push origin v1.1.0

# Breaking change
git tag -a v2.0.0 -m "Breaking: New API for StateMachine"
git push origin v2.0.0
```

## Deleting a Tag (if needed)

If you created a tag by mistake:

```bash
# Delete locally
git tag -d v1.0.1

# Delete on GitHub
git push origin :refs/tags/v1.0.1
```

## Notes

- The release workflow runs **only** when a tag with format `v*.*.*` is pushed
- The generated JAR will automatically have the correct version number
- The changelog is automatically generated from commit messages between the last tag and the current one
- For the first release, the changelog will include all commits since repository creation
