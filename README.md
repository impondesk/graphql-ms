# graphql-ms

## Branch Protection Rules

Direct pushes to `main` are not allowed. All changes must go through a pull request and receive **at least one approving review** before merging.

### Setting Up Branch Protection (Repository Admin Required)

1. Go to **Settings → Branches** in this repository.
2. Under **Branch protection rules**, click **Add rule**.
3. Set **Branch name pattern** to `main`.
4. Enable the following options:
   - ✅ **Require a pull request before merging**
     - ✅ **Require approvals** → set to `1`
     - ✅ **Dismiss stale pull request approvals when new commits are pushed**
     - ✅ **Require review from Code Owners** (uses `.github/CODEOWNERS`)
   - ✅ **Require status checks to pass before merging** *(if CI is configured)*
   - ✅ **Do not allow bypassing the above settings**
5. Click **Save changes**.

### Code Owners

The `.github/CODEOWNERS` file defines who must review pull requests. Any PR modifying files in this repo requires approval from a member of `@impondesk` before it can be merged.