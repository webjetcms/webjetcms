#foreach( $entry in $results.entrySet() )
    <div class="col-md-12">
        <h2>$entry.key</h2>
    </div>
    <table cellspacing="0" class="sort_table" cellpadding="1">
        <thead>
        <tr>
            <th>
                ID
            </th>
            <th>
                Hodnota
            </th>
        </tr>
        </thead>
        <tbody>
            #foreach( $result in $entry.value)
            <tr>
                <td>
                    $result.id
                </td>
                <td>
                    <a href="$result.link" target="_blank">$result.text</a>
                </td>
            </tr>
            #end
        </tbody>
    </table>
#end