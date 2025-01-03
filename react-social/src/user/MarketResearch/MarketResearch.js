import React, { useState } from 'react';
import axios from 'axios';
import Papa from 'papaparse';
import { API_BASE_URL, ACCESS_TOKEN } from '../../constants';
import './MarketResearch.css';

const MarketResearch = () => {
    const [data, setData] = useState([]);
    const [filteredData, setFilteredData] = useState([]);
    const [filterQuery, setFilterQuery] = useState('');
    const [sortColumn, setSortColumn] = useState(null);
    const [sortOrder, setSortOrder] = useState('asc');

    const fetchData = async () => {
        try {
            // Prepare headers with Bearer token
            const headers = new Headers({
                'Content-Type': 'application/json',
            });

            if (localStorage.getItem(ACCESS_TOKEN)) {
                headers.append('Authorization', 'Bearer ' + localStorage.getItem(ACCESS_TOKEN));
            }

            // Send the request using axios
            const response = await axios.post(
                `${API_BASE_URL}/api/download-json`,
                { address: 'Plano' },
                {
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${localStorage.getItem(ACCESS_TOKEN)}`,
                    },
                    responseType: 'blob', // Expect a file
                }
            );

            // Process the response
            const file = response.data;
            const reader = new FileReader();

            reader.onload = () => {
                const csvData = reader.result;
                const parsedData = Papa.parse(csvData, { header: true }).data;
                setData(parsedData);
                setFilteredData(parsedData);
            };

            reader.readAsText(file);
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    };

    const handleFilter = (query) => {
        setFilterQuery(query);
        const filtered = data.filter((row) =>
            Object.values(row).some((value) =>
                value.toString().toLowerCase().includes(query.toLowerCase())
            )
        );
        setFilteredData(filtered);
    };

    const handleSort = (column) => {
        const order = sortColumn === column && sortOrder === 'asc' ? 'desc' : 'asc';
        setSortColumn(column);
        setSortOrder(order);

        const sorted = [...filteredData].sort((a, b) => {
            if (a[column] < b[column]) return order === 'asc' ? -1 : 1;
            if (a[column] > b[column]) return order === 'asc' ? 1 : -1;
            return 0;
        });

        setFilteredData(sorted);
    };

    return (
        <div className="market-research-container">
            <h1>Market Research - Plano</h1>
            <button onClick={fetchData}>Fetch Data</button>
            <input
                type="text"
                placeholder="Filter data"
                value={filterQuery}
                onChange={(e) => handleFilter(e.target.value)}
            />
            {filteredData.length > 0 && (
                <table>
                    <thead>
                        <tr>
                            {Object.keys(filteredData[0]).map((column) => (
                                <th key={column} onClick={() => handleSort(column)}>
                                    {column} {sortColumn === column ? (sortOrder === 'asc' ? '↑' : '↓') : ''}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {filteredData.map((row, index) => (
                            <tr key={index}>
                                {Object.values(row).map((value, i) => (
                                    <td key={i}>{value}</td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    );
};

export default MarketResearch;