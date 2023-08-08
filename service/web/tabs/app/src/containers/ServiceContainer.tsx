import * as React from "react"
import { connect } from "react-redux"
import {
  IDispatchProps,
  IState,
  mapDispatchToProps,
  mapStateToProps
} from "../ducks"
import { H2, H3, Intent, AnchorButton, Spinner } from "@blueprintjs/core"
import { BackfillRunsTable } from "../components"
import { simpleSelectorGet } from "@misk/simpleredux"
import { Link } from "react-router-dom"
import { LayoutContainer } from "."

class ServiceContainer extends React.Component<
  IState & IDispatchProps,
  IState
> {
  private service: string = (this.props as any).match.params.service
  private flavor: string = (this.props as any).match.params.flavor
  private backfillRunsTag: string = `${this.service}::${this.flavor}::BackfillRuns`

  componentDidMount() {
    this.props.simpleNetworkGet(
      this.backfillRunsTag,
      `/services/${this.service}/backfill-runs?flavor=${this.flavor}`
    )
  }

  render() {
    let result = simpleSelectorGet(this.props.simpleNetwork, [
      this.backfillRunsTag,
      "data"
    ])
    if (!this.service || !result) {
      return (
        <LayoutContainer>
          <H2>{this.service} ({this.flavor})</H2>
          <Spinner />
        </LayoutContainer>
      )
    }
    return (
      <LayoutContainer>
        <H2> {this.service} ({this.flavor}) </H2>
        <H3>
          <Link to={`/app/services/${this.service}/flavors`}>
            Other flavors
          </Link>
        </H3>
        <Link to={`/app/services/${this.service}/flavors/${this.flavor}/create`}>
          <AnchorButton text={"Create"} intent={Intent.PRIMARY} />
        </Link>
        <br />
        <br />
        <H3>Running Backfills</H3>
        <BackfillRunsTable backfillRuns={result.running_backfills} />
        <H3>Paused Backfills</H3>
        <BackfillRunsTable backfillRuns={result.paused_backfills} />
        {result.next_pagination_token && (
          <div style={{ paddingBottom: "100px" }}>
            <Link
              to={`/app/services/${this.service}/flavors/${this.flavor}/runs/${result.next_pagination_token}`}
            >
              more
            </Link>
          </div>
        )}
      </LayoutContainer>
    )
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(ServiceContainer)
