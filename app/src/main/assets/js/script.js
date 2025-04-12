<script>
    // Highlight row on hover
    document.querySelectorAll('tr').forEach(row => {
        row.addEventListener('mouseover', () => {
            row.style.backgroundColor = '#f2f2f2';
        });
        row.addEventListener('mouseout', () => {
            row.style.backgroundColor = '';
        });
    });

    // Select cell on click
    document.querySelectorAll('td').forEach(cell => {
        cell.addEventListener('click', () => {
            // Deselect all cells
            document.querySelectorAll('td').forEach(c => c.classList.remove('active-cell'));

            // Select the clicked cell
            cell.classList.add('active-cell');
        });
    });

    // Highlight column on click
    document.querySelectorAll('th').forEach((header, index) => {
        header.addEventListener('click', () => {
            // Highlight entire column
            document.querySelectorAll('td').forEach((cell, cellIndex) => {
                if (cellIndex % document.querySelector('tr').children.length === index) {
                    cell.classList.add('selected');
                }
            });
        });
    });
</script>
