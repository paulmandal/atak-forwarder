- update any image links to use the permanent link style: `RELEASE_VERSION=x.y.z bash -c 'sed -i "s#https://github.com/paulmandal/atak-forwarder/raw/[.0-9a-z]\+/images/#https://github.com/paulmandal/atak-forwarder/raw/${RELEASE_VERSION}/images/#" README.md'`
- commit, PR, and merge updated image links
- tag the release commit on main

